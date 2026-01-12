package com.qyl.v2trade.market.aggregation.core.impl;

import com.qyl.v2trade.common.util.TimeUtil;
import com.qyl.v2trade.market.aggregation.config.SupportedPeriod;
import com.qyl.v2trade.market.aggregation.core.AggregationBucket;
import com.qyl.v2trade.market.aggregation.core.AggregationMetrics;
import com.qyl.v2trade.market.aggregation.core.AggregationStats;
import com.qyl.v2trade.market.aggregation.core.KlineAggregator;
import com.qyl.v2trade.market.aggregation.core.PeriodCalculator;
import com.qyl.v2trade.market.aggregation.event.AggregatedKLine;
import com.qyl.v2trade.market.aggregation.persistence.AggregatedKLineStorageService;
import com.qyl.v2trade.market.calibration.trigger.BackfillTrigger;
import com.qyl.v2trade.market.model.NormalizedKline;
import com.qyl.v2trade.market.model.event.KlineEvent;
import com.qyl.v2trade.market.web.query.MarketQueryService;
import com.qyl.v2trade.business.system.service.TradingPairService;
import com.qyl.v2trade.business.system.model.entity.TradingPair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * K线聚合器实现
 * 
 * <p>负责将1m K线事件聚合成多周期K线
 *
 * @author qyl
 */
@Slf4j
public class KlineAggregatorImpl implements KlineAggregator {
    
    /**
     * 聚合结果回调（用于事件发布，在任务2.2中实现）
     * 
     * <p>当聚合完成时，会调用此回调函数
     */
    private Consumer<AggregatedKLine> aggregationCallback;
    
    /**
     * 存储服务（可选，如果为null则不写入数据库）
     */
    private AggregatedKLineStorageService storageService;
    
    /**
     * 市场查询服务（用于查询QuestDB中的历史1m K线数据）
     */
    @Autowired(required = false)
    @Qualifier("questDbMarketQueryService")
    private MarketQueryService marketQueryService;

    /**
     * 补拉触发器
     */
    @Autowired(required = false)
    private BackfillTrigger backfillTrigger;

    /**
     * 交易对服务（用于获取tradingPairId）
     */
    @Autowired(required = false)
    private TradingPairService tradingPairService;

    /**
     * 最大等待时间（毫秒），默认500ms
     */
    @Value("${calibration.backfill.maxWaitMillis:500}")
    private int maxWaitMillis;
    
    /**
     * 异步写入线程池（用于不阻塞聚合流程）
     */
    private final ExecutorService writeExecutor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "Aggregation-Write-Worker");
        t.setDaemon(true);
        return t;
    });
    
    /**
     * Bucket存储：Key格式为 {symbol}_{period}_{windowStart}
     */
    private final ConcurrentHashMap<String, AggregationBucket> buckets = new ConcurrentHashMap<>();
    
    /**
     * 统计信息：总处理的K线事件数量
     */
    private final AtomicLong totalEventCount = new AtomicLong(0);
    
    /**
     * 统计信息：总生成的聚合K线数量
     */
    private final AtomicLong totalAggregatedCount = new AtomicLong(0);
    
    /**
     * 监控指标
     */
    private final AggregationMetrics metrics = new AggregationMetrics();
    
    /**
     * 去重集合：记录已处理的K线（用于去重）
     * Key格式：{symbol}_{period}_{windowStart}_{klineOpenTime}
     */
    private final ConcurrentHashMap<String, Boolean> processedKlines = new ConcurrentHashMap<>();
    
    /**
     * 每个symbol的最后处理时间戳（用于时间乱序检测）
     * Key格式：{symbol}_{period}
     */
    private final ConcurrentHashMap<String, Long> lastProcessedTimestamp = new ConcurrentHashMap<>();
    
    /**
     * 构造函数
     */
    public KlineAggregatorImpl() {
        this(null, null);
    }
    
    /**
     * 构造函数
     * 
     * @param aggregationCallback 聚合完成回调（可选）
     */
    public KlineAggregatorImpl(Consumer<AggregatedKLine> aggregationCallback) {
        this(aggregationCallback, null);
    }
    
    /**
     * 构造函数
     * 
     * @param aggregationCallback 聚合完成回调（可选）
     * @param storageService 存储服务（可选）
     */
    public KlineAggregatorImpl(Consumer<AggregatedKLine> aggregationCallback, 
                               AggregatedKLineStorageService storageService) {
        this.aggregationCallback = aggregationCallback;
        this.storageService = storageService;
    }
    
    /**
     * 设置聚合完成回调
     * 
     * @param callback 回调函数
     */
    public void setAggregationCallback(Consumer<AggregatedKLine> callback) {
        this.aggregationCallback = callback;
    }
    
    /**
     * 设置存储服务
     * 
     * @param storageService 存储服务
     */
    public void setStorageService(AggregatedKLineStorageService storageService) {
        this.storageService = storageService;
    }
    
    @Override
    public void onKlineEvent(KlineEvent event) {
        long startTime = System.nanoTime();
        
        try {
            // 只处理1m K线
            if (!"1m".equals(event.interval())) {
                log.debug("跳过非1m K线事件: interval={}", event.interval());
                return;
            }
            
            totalEventCount.incrementAndGet();
            metrics.incrementEventCount();
            
            // 遍历所有支持的周期
            for (SupportedPeriod period : SupportedPeriod.values()) {
                processKlineForPeriod(event, period);
            }
            
            // 记录聚合延迟
            long latencyNs = System.nanoTime() - startTime;
            metrics.recordAggregationLatency(latencyNs);
            metrics.incrementSuccessCount();
            
        } catch (Exception e) {
            metrics.incrementFailCount();
            log.error("处理K线事件异常: symbol={}, interval={}, openTime={}", 
                    event.symbol(), event.interval(), event.openTime(), e);
        }
    }
    
    /**
     * 处理单个周期的K线聚合
     */
    private void processKlineForPeriod(KlineEvent event, SupportedPeriod period) {
        try {
            // 1. 计算该K线所属的聚合窗口
            long openTimeMillis = TimeUtil.toEpochMilli(event.openTime());
            long windowStart = PeriodCalculator.calculateWindowStart(openTimeMillis, period);
            long windowEnd = PeriodCalculator.calculateWindowEnd(windowStart, period);
            
            // 2. 生成Bucket Key
            String bucketKey = generateBucketKey(event.symbol(), period.getPeriod(), windowStart);
            
            // 3. 检查去重
            String klineKey = generateKlineKey(event.symbol(), period.getPeriod(), windowStart, openTimeMillis);
            if (processedKlines.containsKey(klineKey)) {
                log.debug("跳过重复K线: symbol={}, period={}, openTime={}", 
                        event.symbol(), period.getPeriod(), TimeUtil.formatWithBothTimezones(event.openTime()));
                metrics.incrementDuplicateIgnoreCount();
                return;
            }

            String timestampKey = generateTimestampKey(event.symbol(), period.getPeriod());

            
            // 5. 找到或创建Bucket
            // 使用computeIfAbsent确保线程安全，同时判断是否是新创建的Bucket
            AggregationBucket bucket = buckets.computeIfAbsent(bucketKey, key -> {
                log.debug("创建新Bucket: key={}", key);
                AggregationBucket newBucket = new AggregationBucket(event.symbol(), period.getPeriod(), windowStart, windowEnd);
                Instant windowStartInstant = TimeUtil.fromEpochMilli(windowStart);
                if (event.openTime().isAfter(windowStartInstant) && marketQueryService != null) {
                    // 在创建Bucket后立即补齐缺失的数据
                    backfillMissingKlines(event, period, windowStartInstant, newBucket);
                }
                return newBucket;
            });
            
            // 6. 更新Bucket状态
            boolean windowComplete = bucket.update(event);
            
            // 7. 标记K线已处理（去重）
            processedKlines.put(klineKey, Boolean.TRUE);
            
            // 8. 更新最后处理时间戳
            lastProcessedTimestamp.put(timestampKey, openTimeMillis);
            
            // 9. 如果窗口结束，生成聚合结果
            if (windowComplete) {
                handleWindowComplete(bucket, bucketKey);
            }
            
        } catch (Exception e) {
            log.error("处理K线事件异常: symbol={}, period={}, openTime={}", 
                    event.symbol(), period.getPeriod(), TimeUtil.formatWithBothTimezones(event.openTime()), e);
        }
    }
    
    /**
     * 补齐缺失的1m K线数据
     * 
     * <p>当系统在窗口中间启动时（例如5分钟窗口的03分钟），需要从QuestDB查询窗口开始到当前时间之间的所有1m K线数据
     * 
     * <p>例如：5分钟窗口[10:00, 10:05)，在10:03启动，收到的第一根K线是10:03的
     * 此时需要从QuestDB查询10:00、10:01、10:02的1m K线数据
     * 
     * 重构：按照时间管理约定，使用 Instant 作为参数类型
     * 
     * @param event 当前收到的K线事件
     * @param period 聚合周期
     * @param windowStart 窗口起始时间（UTC Instant）
     * @param bucket 聚合Bucket
     */
    private void backfillMissingKlines(KlineEvent event, SupportedPeriod period, 
                                      Instant windowStart, AggregationBucket bucket) {
        try {
            // 计算需要查询的时间范围：[windowStart, event.openTime())
            Instant queryEndTime = event.openTime(); // 不包含当前K线，因为当前K线会在后面处理
            
            // 从QuestDB查询该时间范围内的所有1m K线数据
            // 重构：按照时间管理约定，直接传递 Instant 参数
            // 数据库查询边界转换在 MarketQueryService 实现类内部完成
            List<NormalizedKline> missingKlines = marketQueryService.queryKlines(
                    event.symbol(), 
                    "1m", 
                    windowStart, 
                    queryEndTime, 
                    null // 不限制数量，查询所有
            );
            
            if (missingKlines == null || missingKlines.isEmpty()) {
                log.debug("QuestDB中无缺失的1m K线数据: symbol={}, period={}, windowStart={}, queryEndTime={}",
                        event.symbol(), period.getPeriod(), TimeUtil.formatWithBothTimezones(windowStart), 
                        TimeUtil.formatWithBothTimezones(queryEndTime));
                return;
            }
            
            log.info("从QuestDB补齐缺失的1m K线数据: symbol={}, period={}, windowStart={}, missingCount={}", 
                    event.symbol(), period.getPeriod(), TimeUtil.formatWithBothTimezones(windowStart), missingKlines.size());
            
            // 将查询到的历史K线数据转换为KlineEvent并聚合到Bucket中
            for (NormalizedKline kline : missingKlines) {
                // 检查是否已处理过（去重）
                String klineKey = generateKlineKey(event.symbol(), period.getPeriod(), TimeUtil.toEpochMilli(windowStart), kline.getTimestamp());
                if (processedKlines.containsKey(klineKey)) {
                    log.debug("跳过已处理的1m K线: symbol={}, timestamp={}", 
                            event.symbol(), TimeUtil.formatWithBothTimezones(kline.getTimestampInstant()));
                    continue;
                }
                
                // 转换为KlineEvent
                KlineEvent historicalEvent = convertToKlineEvent(kline, event.exchange());
                
                // 更新Bucket（不检查窗口完成，因为这是历史数据）
                bucket.update(historicalEvent);
                
                // 标记已处理（去重）
                processedKlines.put(klineKey, Boolean.TRUE);
                
                log.debug("补齐历史1m K线: symbol={}, timestamp={}, open={}, high={}, low={}, close={}, volume={}", 
                        event.symbol(), TimeUtil.formatWithBothTimezones(kline.getTimestampInstant()), 
                        kline.getOpen(), kline.getHigh(), kline.getLow(), kline.getClose(), kline.getVolume());
            }
            
        } catch (Exception e) {
            log.error("补齐缺失的1m K线数据异常: symbol={}, period={}, windowStart={}", 
                    event.symbol(), period.getPeriod(), windowStart, e);
            // 不抛出异常，继续处理当前K线事件
        }
    }
    
    /**
     * 将KlineEvent转换为NormalizedKline
     * 
     * @param event KlineEvent
     * @return NormalizedKline
     */
    private NormalizedKline convertKlineEventToNormalizedKline(KlineEvent event) {
        if (event == null) {
            return null;
        }
        NormalizedKline kline = NormalizedKline.builder()
                .symbol(event.symbol())
                .interval(event.interval())
                .open(event.open().doubleValue())
                .high(event.high().doubleValue())
                .low(event.low().doubleValue())
                .close(event.close().doubleValue())
                .volume(event.volume().doubleValue())
                .build();
        // 使用 setter 方法设置 Instant 类型的时间戳
        kline.setTimestampInstant(event.openTime());
        kline.setExchangeTimestampInstant(event.openTime());
        return kline;
    }
    
    /**
     * 将NormalizedKline转换为KlineEvent
     * 
     * @param kline NormalizedKline
     * @param exchange 交易所名称
     * @return KlineEvent
     */
    private KlineEvent convertToKlineEvent(NormalizedKline kline, String exchange) {
        long openTime = kline.getTimestamp();
        long closeTime = openTime + 60000; // 1分钟K线
        
        // 转换 long 为 Instant
        java.time.Instant openTimeInstant = com.qyl.v2trade.common.util.TimeUtil.fromEpochMilli(openTime);
        java.time.Instant closeTimeInstant = com.qyl.v2trade.common.util.TimeUtil.fromEpochMilli(closeTime);
        
        return KlineEvent.of(
                kline.getSymbol(),
                exchange,
                openTimeInstant,
                closeTimeInstant,
                kline.getInterval(),
                BigDecimal.valueOf(kline.getOpen()),
                BigDecimal.valueOf(kline.getHigh()),
                BigDecimal.valueOf(kline.getLow()),
                BigDecimal.valueOf(kline.getClose()),
                BigDecimal.valueOf(kline.getVolume()),
                true, // 历史数据都是已完成的
                java.time.Instant.now()  // eventTime (UTC Instant)
        );
    }
    
    /**
     * 处理窗口完成
     * 
     * <p>实现"两段读取 + 首尾重算规则"：
     * <ul>
     *   <li>第一次读取：从QuestDB查询1m数据</li>
     *   <li>完整性判断：检查数量、首尾</li>
     *   <li>若不完整：触发补拉，然后进行第二次读取</li>
     *   <li>可选第三次读取：最多一次，有严格时间预算</li>
     *   <li>用最终数据重新计算聚合OHLCV</li>
     *   <li>写入聚合表（只INSERT，不允许UPDATE）</li>
     *   <li>发布事件（带sourceCount）</li>
     * </ul>
     */
    private void handleWindowComplete(AggregationBucket bucket, String bucketKey) {
        try {
            String symbol = bucket.getSymbol();
            String period = bucket.getPeriod();
            long windowStart = bucket.getWindowStart();
            long windowEnd = bucket.getWindowEnd();
            
            // 1. 计算期望的1m K线数量
            SupportedPeriod periodEnum = SupportedPeriod.fromPeriod(period);
            if (periodEnum == null) {
                log.error("不支持的周期: period={}, key={}", period, bucketKey);
                return;
            }
            int expectedCount = (int) (periodEnum.getDurationMs() / 60000); // 分钟数
            // 重构：使用 Instant 进行计算，遵循时间管理约定
            Instant windowStartInstant = TimeUtil.fromEpochMilli(windowStart);
            Instant windowEndInstant = TimeUtil.fromEpochMilli(windowEnd);
            List<NormalizedKline> bars = new ArrayList<>();

            // 4. 若不完整：从QuestDB查询补充数据
            if (bucket.getKlines().size() != expectedCount) {
                bars = query1mBars(symbol, windowStartInstant, windowEndInstant);
            }
            
            // 5. 若仍然不完整：触发补拉，然后进行第二次读取
            if (bars.size() != expectedCount) {
                log.info("bars.size={}", bars.size());
                Long tradingPairId = getTradingPairId(symbol);
                bars = queryDBBars(tradingPairId,symbol, windowStartInstant, windowEndInstant);
            }

            // 6. 用最终拿到的数据计算聚合 OHLCV
            // 重构：使用 Instant 参数，遵循时间管理约定
            AggregatedKLine aggregated = calculateAggregatedKLine(
                    symbol, period, windowStartInstant, bars, expectedCount, bars.size());
            
            if (aggregated == null) {
                log.warn("无法生成聚合结果: symbol={}, period={}, windowStart={}, sourceCount={}", 
                        symbol, period, windowStart, bars.size());
                buckets.remove(bucketKey);
                return;
            }
            
            totalAggregatedCount.incrementAndGet();
            
            // 7. 结构化日志
            if (bars.size() != expectedCount) {
                log.warn("聚合不完整: tradingPairId={}, symbol={}, targetTf={}, windowStart={}, windowEnd={}, " +
                        "expectedCount={}, sourceCount={}",
                        getTradingPairId(symbol), symbol, period, windowStart, windowEnd,
                        expectedCount, bars.size());
            }
            
            log.debug("窗口聚合完成: symbol={}, period={}, timestamp={}, sourceCount={}, expectedCount={}",
                    symbol, period, aggregated.timestamp(),  bars.size(), expectedCount);
            
            // 8. 异步写入QuestDB（不阻塞聚合流程，只INSERT，不允许UPDATE）
            if (storageService != null) {
                writeExecutor.submit(() -> {
                    try {
                        boolean saved = storageService.save(aggregated);
                        if (saved) {
                            log.debug("聚合K线已写入QuestDB: symbol={}, period={}, timestamp={}", 
                                    aggregated.symbol(), aggregated.period(), aggregated.timestamp());
                            metrics.incrementWriteSuccessCount();
                        } else {
                            log.debug("聚合K线写入跳过（已存在）: symbol={}, period={}, timestamp={}", 
                                    aggregated.symbol(), aggregated.period(), aggregated.timestamp());
                            metrics.incrementWriteSkipCount();
                        }
                    } catch (Exception e) {
                        log.error("写入聚合K线到QuestDB异常: symbol={}, period={}, timestamp={}", 
                                aggregated.symbol(), aggregated.period(), aggregated.timestamp(), e);
                        metrics.incrementWriteFailCount();
                        // 写入失败不影响后续聚合
                    }
                });
            }
            
            // 9. 发布事件（如果回调函数存在，带sourceCount）
            if (aggregationCallback != null) {
                try {
                    aggregationCallback.accept(aggregated);
                } catch (Exception e) {
                    log.error("发布聚合事件异常: symbol={}, period={}, timestamp={}", 
                            aggregated.symbol(), aggregated.period(), aggregated.timestamp(), e);
                }
            }
            
            // 10. 清理Bucket
            buckets.remove(bucketKey);
            
        } catch (Exception e) {
            log.error("处理窗口完成异常: key={}", bucketKey, e);
        }
    }
    
    /**
     * 查询1m K线数据
     * 
     * 重构：按照时间管理约定，使用 Instant 作为参数类型
     * 
     * @param symbol 交易对符号
     * @param windowStart 窗口起始时间（UTC Instant）
     * @param windowEnd 窗口结束时间（UTC Instant）
     * @return 1m K线列表
     */
    private List<NormalizedKline> query1mBars(String symbol, Instant windowStart, Instant windowEnd) {
        if (marketQueryService == null) {
            return new ArrayList<>();
        }
        try {
            // 重构：按照时间管理约定，直接传递 Instant 参数
            // 数据库查询边界转换在 MarketQueryService 实现类内部完成
            return marketQueryService.queryKlines(symbol, "1m", windowStart, windowEnd, null);
        } catch (Exception e) {
            log.error("查询1m K线数据失败: symbol={}, windowStart={}, windowEnd={}", 
                    symbol, TimeUtil.formatWithBothTimezones(windowStart), TimeUtil.formatWithBothTimezones(windowEnd), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取 tradingPairId
     */
    private Long getTradingPairId(String symbol) {
        if (tradingPairService == null) {
            return null;
        }
        try {
            TradingPair tradingPair = tradingPairService.getBySymbolAndMarketType(symbol, "SWAP");
            return tradingPair != null ? tradingPair.getId() : null;
        } catch (Exception e) {
            log.warn("获取tradingPairId失败: symbol={}", symbol, e);
            return null;
        }
    }


    private List<NormalizedKline> queryDBBars(Long tradingPairId, String symbol,  Instant windowStartInstant,Instant windowEndInstant) {
        if (tradingPairId == null) {
           return new ArrayList<>();
        }
        // 触发补拉（异步，不阻塞）
        // 重构：使用 Instant 参数，遵循时间管理约定
        backfillTrigger.triggerLast1Hour(
                tradingPairId,
                symbol,
                BackfillTrigger.BackfillReason.INCOMPLETE_AGG_SOURCE,
                windowEndInstant
        );
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // 立即进行第二次读取（重查）
        return query1mBars(symbol, windowStartInstant, windowEndInstant);
    }
    /**
     * 计算聚合K线
     * 
     * <p>规则：
     * <ul>
     *   <li>open：优先 windowStart 那根的 open；若缺失，降级为最早bar的 open</li>
     *   <li>close：优先 lastMinuteTs 那根的 close；若缺失，降级为最晚bar的 close</li>
     *   <li>high：max(high)</li>
     *   <li>low：min(low)</li>
     *   <li>volume：sum(volume)</li>
     * </ul>
     */
    private AggregatedKLine calculateAggregatedKLine(String symbol, String period, Instant windowStart,
                                                     List<NormalizedKline> bars, int expectedCount, 
                                                     int sourceCount) {
        if (bars.isEmpty()) {
            return null;
        }
        
        SupportedPeriod periodEnum = SupportedPeriod.fromPeriod(period);
        if (periodEnum == null) {
            return null;
        }
        
        // 重构：使用 Instant 进行计算，遵循时间管理约定
        long windowStartMillis = TimeUtil.toEpochMilli(windowStart);
        long alignedTimestamp = PeriodCalculator.alignTimestamp(windowStartMillis, periodEnum);
        
        // 找到首尾bar
        // 重构：使用 Instant 进行比较，遵循时间管理约定
        NormalizedKline startBar = null;
        NormalizedKline endBar = null;
        // 如果没有找到首尾，使用最早/最晚的bar
        if (startBar == null && !bars.isEmpty()) {
            startBar = bars.get(0);
        }
        if (endBar == null && !bars.isEmpty()) {
            endBar = bars.get(bars.size() - 1);
        }
        
        if (startBar == null || endBar == null) {
            return null;
        }
        // 计算 OHLCV
        BigDecimal open = BigDecimal.valueOf(startBar.getOpen());
        BigDecimal close = BigDecimal.valueOf(endBar.getClose());
        BigDecimal high = bars.stream()
                .map(b -> BigDecimal.valueOf(b.getHigh()))
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        BigDecimal low = bars.stream()
                .map(b -> BigDecimal.valueOf(b.getLow()))
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        BigDecimal volume = bars.stream()
                .map(b -> BigDecimal.valueOf(b.getVolume()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return AggregatedKLine.of(
                symbol,
                period,
                alignedTimestamp,
                open,
                high,
                low,
                close,
                volume,
                sourceCount
        );
    }
    
    @Override
    public AggregationStats getStats() {
        return AggregationStats.of(
                buckets.size(),
                totalEventCount.get(),
                totalAggregatedCount.get()
        );
    }
    
    /**
     * 获取监控指标
     * 
     * @return 监控指标
     */
    public AggregationMetrics getMetrics() {
        return metrics;
    }
    
    @Override
    public void cleanupExpiredBuckets() {
        long currentTime = System.currentTimeMillis();
        List<String> expiredKeys = new ArrayList<>();
        
        // 收集过期的Bucket
        buckets.forEach((key, bucket) -> {
            if (bucket.isExpired(currentTime)) {
                expiredKeys.add(key);
            }
        });
        
        // 清理过期Bucket
        for (String key : expiredKeys) {
            AggregationBucket removed = buckets.remove(key);
            if (removed != null) {
                log.debug("清理过期Bucket: key={}, symbol={}, period={}", 
                        key, removed.getSymbol(), removed.getPeriod());
            }
        }
        
        if (!expiredKeys.isEmpty()) {
            log.info("清理过期Bucket完成: 清理数量={}, 剩余Bucket数量={}", 
                    expiredKeys.size(), buckets.size());
        }
        
        // 清理过期的去重记录（避免内存泄漏）
        // 策略：清理所有记录的1/10（简单策略，可以优化）
        if (processedKlines.size() > 10000) {
            int targetSize = processedKlines.size() * 9 / 10;
            // 简单策略：清除所有记录，重新开始（实际场景中可以使用LRU等策略）
            processedKlines.clear();
            log.debug("清理去重记录缓存: 目标大小={}", targetSize);
        }
        
        // 清理过期的时间戳记录（避免内存泄漏）
        // 策略：如果记录数量过多，清理一部分
        lastProcessedTimestamp.entrySet().removeIf(entry -> {
            return lastProcessedTimestamp.size() > 1000;
        });
    }
    

    

    
    /**
     * 生成Bucket Key
     * 
     * @param symbol 交易对符号
     * @param period 周期字符串
     * @param windowStart 窗口起始时间戳
     * @return Bucket Key
     */
    private String generateBucketKey(String symbol, String period, long windowStart) {
        return symbol + "_" + period + "_" + windowStart;
    }
    
    /**
     * 生成K线去重Key
     * 
     * @param symbol 交易对符号
     * @param period 周期字符串
     * @param windowStart 窗口起始时间戳
     * @param klineOpenTime K线开盘时间戳
     * @return K线Key
     */
    private String generateKlineKey(String symbol, String period, long windowStart, long klineOpenTime) {
        return symbol + "_" + period + "_" + windowStart + "_" + klineOpenTime;
    }
    
    /**
     * 生成时间戳Key（用于时间乱序检测）
     * 
     * @param symbol 交易对符号
     * @param period 周期字符串
     * @return 时间戳Key
     */
    private String generateTimestampKey(String symbol, String period) {
        return symbol + "_" + period;
    }
    
    /**
     * 获取所有活跃的Bucket（用于测试和监控）
     * 
     * @return Bucket集合的副本
     */
    public Set<String> getActiveBucketKeys() {
        return Set.copyOf(buckets.keySet());
    }
}

