package com.qyl.v2trade.market.aggregation.core.impl;

import com.qyl.v2trade.market.aggregation.config.SupportedPeriod;
import com.qyl.v2trade.market.aggregation.core.AggregationBucket;
import com.qyl.v2trade.market.aggregation.core.AggregationMetrics;
import com.qyl.v2trade.market.aggregation.core.AggregationStats;
import com.qyl.v2trade.market.aggregation.core.KlineAggregator;
import com.qyl.v2trade.market.aggregation.core.PeriodCalculator;
import com.qyl.v2trade.market.aggregation.event.AggregatedKLine;
import com.qyl.v2trade.market.aggregation.persistence.AggregatedKLineStorageService;
import com.qyl.v2trade.market.model.event.KlineEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
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
            long windowStart = PeriodCalculator.calculateWindowStart(event.openTime(), period);
            long windowEnd = PeriodCalculator.calculateWindowEnd(windowStart, period);
            
            // 2. 生成Bucket Key
            String bucketKey = generateBucketKey(event.symbol(), period.getPeriod(), windowStart);
            
            // 3. 检查去重
            String klineKey = generateKlineKey(event.symbol(), period.getPeriod(), windowStart, event.openTime());
            if (processedKlines.containsKey(klineKey)) {
                log.debug("跳过重复K线: symbol={}, period={}, openTime={}", 
                        event.symbol(), period.getPeriod(), event.openTime());
                metrics.incrementDuplicateIgnoreCount();
                return;
            }

            String timestampKey = generateTimestampKey(event.symbol(), period.getPeriod());

            
            // 5. 找到或创建Bucket
            AggregationBucket bucket = buckets.computeIfAbsent(bucketKey, key -> {
                log.debug("创建新Bucket: key={}", key);
                return new AggregationBucket(event.symbol(), period.getPeriod(), windowStart, windowEnd);
            });
            
            // 6. 更新Bucket状态
            boolean windowComplete = bucket.update(event);
            
            // 7. 标记K线已处理（去重）
            processedKlines.put(klineKey, Boolean.TRUE);
            
            // 8. 更新最后处理时间戳
            lastProcessedTimestamp.put(timestampKey, event.openTime());
            
            // 9. 如果窗口结束，生成聚合结果
            if (windowComplete) {
                handleWindowComplete(bucket, bucketKey);
            }
            
        } catch (Exception e) {
            log.error("处理K线事件异常: symbol={}, period={}, openTime={}", 
                    event.symbol(), period.getPeriod(), event.openTime(), e);
        }
    }
    
    /**
     * 处理窗口完成
     */
    private void handleWindowComplete(AggregationBucket bucket, String bucketKey) {
        try {
            // 生成聚合结果
            AggregatedKLine aggregated = bucket.toAggregatedKLine();
            if (aggregated == null) {
                log.warn("Bucket为空，不生成聚合结果: key={}", bucketKey);
                return;
            }
            
            totalAggregatedCount.incrementAndGet();
            
            log.debug("窗口聚合完成: symbol={}, period={}, timestamp={}, klineCount={}", 
                    aggregated.symbol(), aggregated.period(), aggregated.timestamp(), aggregated.sourceKlineCount());
            
            // 异步写入QuestDB（不阻塞聚合流程）
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
            
            // 发布事件（如果回调函数存在）
            if (aggregationCallback != null) {
                try {
                    aggregationCallback.accept(aggregated);
                } catch (Exception e) {
                    log.error("发布聚合事件异常: symbol={}, period={}, timestamp={}", 
                            aggregated.symbol(), aggregated.period(), aggregated.timestamp(), e);
                }
            }
            
            // 清理Bucket
            buckets.remove(bucketKey);
            
            // 清理相关的去重记录（可选，避免内存泄漏）
            // 注意：这里只清理当前Bucket的K线记录，其他记录由cleanupExpiredBuckets清理
            
        } catch (Exception e) {
            log.error("处理窗口完成异常: key={}", bucketKey, e);
        }
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
        // 策略：清理超过24小时的记录
        long expireTime = currentTime - (24 * 60 * 60 * 1000L);
        lastProcessedTimestamp.entrySet().removeIf(entry -> {
            // 简单策略：如果记录数量过多，清理一部分
            return lastProcessedTimestamp.size() > 1000;
        });
    }
    
    @Override
    public void initializeHistoryBackfill(List<String> symbols) {
        // 此方法由KlineAggregatorInitializer调用，实际实现在Initializer中
        log.info("initializeHistoryBackfill方法由KlineAggregatorInitializer调用: symbols={}", 
                symbols != null ? symbols.size() : "null");
    }
    
    @Override
    public void backfillWindow(String symbol, SupportedPeriod period, long windowStart) {
        // 此方法由KlineAggregatorInitializer调用，实际实现在Initializer中
        log.info("backfillWindow方法由KlineAggregatorInitializer调用: symbol={}, period={}, windowStart={}", 
                symbol, period.getPeriod(), windowStart);
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

