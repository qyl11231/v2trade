package com.qyl.v2trade.market.aggregation.core.impl;

import com.qyl.v2trade.business.system.model.entity.MarketSubscriptionConfig;
import com.qyl.v2trade.business.system.model.entity.TradingPair;
import com.qyl.v2trade.business.system.service.MarketSubscriptionConfigService;
import com.qyl.v2trade.business.system.service.TradingPairService;
import com.qyl.v2trade.market.aggregation.config.AggregationProperties;
import com.qyl.v2trade.market.aggregation.config.SupportedPeriod;
import com.qyl.v2trade.market.aggregation.core.AggregationBucket;
import com.qyl.v2trade.market.aggregation.core.KlineAggregator;
import com.qyl.v2trade.market.aggregation.core.PeriodCalculator;
import com.qyl.v2trade.market.aggregation.event.AggregatedKLine;
import com.qyl.v2trade.market.aggregation.persistence.AggregatedKLineStorageService;
import com.qyl.v2trade.market.model.NormalizedKline;
import com.qyl.v2trade.market.model.event.KlineEvent;
import com.qyl.v2trade.market.web.query.MarketQueryService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * K线聚合器初始化器
 * 
 * <p>负责系统启动时的历史数据补齐
 *
 * @author qyl
 */
@Slf4j
@Component
public class KlineAggregatorInitializer {
    
    @Autowired
    private KlineAggregator klineAggregator;
    
    @Autowired
    private AggregationProperties aggregationProperties;
    
    @Autowired(required = false)
    private AggregatedKLineStorageService storageService;
    
    @Autowired
    @Qualifier("questDbMarketQueryService")
    private MarketQueryService marketQueryService;
    
    @Autowired
    private TradingPairService tradingPairService;
    
    @Autowired
    private MarketSubscriptionConfigService marketSubscriptionConfigService;
    
    /**
     * 异步执行线程池
     */
    private final ExecutorService executorService = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "Aggregation-Initializer-Worker");
        t.setDaemon(true);
        return t;
    });
    
    @PostConstruct
    public void initialize() {
        if (!aggregationProperties.isEnableHistoryBackfill()) {
            log.info("历史数据补齐已禁用，跳过初始化");
            return;
        }
        
        if (storageService == null) {
            log.warn("存储服务未配置，跳过历史数据补齐");
            return;
        }
        
        if (aggregationProperties.isAsyncInitialization()) {
            // 异步执行，不阻塞启动
            executorService.submit(() -> {
                try {
                    doInitialize(null);
                } catch (Exception e) {
                    log.error("历史数据补齐初始化失败", e);
                }
            });
            log.info("历史数据补齐已启动（异步执行）");
        } else {
            // 同步执行
            try {
                doInitialize(null);
            } catch (Exception e) {
                log.error("历史数据补齐初始化失败", e);
            }
        }
    }
    
    /**
     * 执行初始化
     * 
     * @param symbols 需要补齐的交易对列表（如果为空，则补齐所有启用的交易对）
     */
    public void doInitialize(List<String> symbols) {
        log.info("开始执行历史数据补齐初始化");
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 获取所有需要聚合的交易对
            List<String> targetSymbols = getTargetSymbols(symbols);
            if (targetSymbols.isEmpty()) {
                log.warn("没有需要补齐的交易对");
                return;
            }
            
            log.info("需要补齐的交易对数量: {}", targetSymbols.size());
            
            // 2. 遍历每个交易对和周期
            int totalWindows = 0;
            int backfilledWindows = 0;
            
            for (String symbol : targetSymbols) {
                for (SupportedPeriod period : SupportedPeriod.values()) {
                    // 3. 扫描未完成的窗口
                    List<Long> incompleteWindows = scanIncompleteWindows(symbol, period);
                    totalWindows += incompleteWindows.size();
                    
                    // 4. 补齐每个窗口
                    for (Long windowStart : incompleteWindows) {
                        // 确保windowStart已对齐到周期边界
                        long alignedWindowStart = PeriodCalculator.alignTimestamp(windowStart, period);
                        if (backfillWindow(symbol, period, alignedWindowStart)) {
                            backfilledWindows++;
                        }
                    }
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("历史数据补齐初始化完成: 交易对数量={}, 总窗口数={}, 补齐窗口数={}, 耗时={}ms", 
                    targetSymbols.size(), totalWindows, backfilledWindows, duration);
            
        } catch (Exception e) {
            log.error("历史数据补齐初始化异常", e);
        }
    }
    
    /**
     * 获取目标交易对列表
     * 
     * <p>基于行情订阅配置表获取需要聚合的交易对
     * <p>注意：同一个symbol可能有多个marketType（SPOT/SWAP），需要区分处理
     * 
     * @param symbols 如果指定了symbols，则直接返回（用于手动触发补齐）
     * @return 交易对列表（格式：symbol 或 symbol-marketType，用于区分SPOT/SWAP）
     */
    private List<String> getTargetSymbols(List<String> symbols) {
        if (symbols != null && !symbols.isEmpty()) {
            return symbols;
        }
        
        // 从行情订阅配置表获取启用的订阅配置
        List<MarketSubscriptionConfig> configs = marketSubscriptionConfigService.listEnabled();
        if (configs.isEmpty()) {
            log.warn("没有启用的行情订阅配置");
            return new ArrayList<>();
        }
        
        List<String> targetSymbols = new ArrayList<>();
        for (MarketSubscriptionConfig config : configs) {
            try {
                // 获取交易对信息
                TradingPair tradingPair = tradingPairService.getById(config.getTradingPairId());
                if (tradingPair == null) {
                    log.warn("交易对不存在: tradingPairId={}", config.getTradingPairId());
                    continue;
                }
                
                // 生成唯一标识：symbol-marketType（用于区分SPOT/SWAP）
                // 例如：BTC-USDT-SPOT, BTC-USDT-SWAP
                String symbolKey = tradingPair.getSymbol() + "-" + tradingPair.getMarketType();
                targetSymbols.add(symbolKey);
                
                log.debug("添加聚合目标: symbol={}, marketType={}, symbolKey={}", 
                        tradingPair.getSymbol(), tradingPair.getMarketType(), symbolKey);
            } catch (Exception e) {
                log.error("获取交易对信息失败: tradingPairId={}", config.getTradingPairId(), e);
            }
        }
        
        return targetSymbols.stream().distinct().collect(Collectors.toList());
    }
    
    /**
     * 扫描需要补齐的历史窗口
     * 
     * @param symbol 交易对
     * @param period 周期
     * @return 需要补齐的窗口列表（窗口起始时间戳）
     */
    public List<Long> scanIncompleteWindows(String symbol, SupportedPeriod period) {
        List<Long> incompleteWindows = new ArrayList<>();
        
        long currentTime = System.currentTimeMillis();
        
        // 计算当前窗口的起始时间
        long currentWindowStart = PeriodCalculator.calculateWindowStart(currentTime, period);
        
        // 扫描时间范围：从最早可能的时间到当前窗口
        // 限制：最多扫描最近N小时（可配置）
        long scanStartTime = currentTime - (aggregationProperties.getHistoryScanHours() * 60 * 60 * 1000L);
        long scanWindowStart = PeriodCalculator.calculateWindowStart(scanStartTime, period);
        
        // 遍历所有可能的窗口
        for (long windowStart = scanWindowStart; windowStart < currentWindowStart; windowStart += period.getDurationMs()) {
            // 【第一层检查】检查该窗口是否已有聚合数据
            if (!storageService.exists(symbol, period.getPeriod(), windowStart)) {
                incompleteWindows.add(windowStart);
            }
        }
        
        if (!incompleteWindows.isEmpty()) {
            log.debug("扫描到未完成的窗口: symbol={}, period={}, 窗口数量={}", 
                    symbol, period.getPeriod(), incompleteWindows.size());
        }
        
        return incompleteWindows;
    }
    
    /**
     * 补齐指定窗口的历史数据
     * 
     * <p>注意：symbol 格式为 symbol-marketType（如 BTC-USDT-SPOT, BTC-USDT-SWAP）
     * 
     * @param symbol 交易对（格式：symbol-marketType）
     * @param period 周期
     * @param windowStart 窗口起始时间戳（必须已对齐到周期边界）
     * @return 是否成功补齐
     */
    public boolean backfillWindow(String symbol, SupportedPeriod period, long windowStart) {
        try {
            // 【第二层检查】检查该窗口的聚合数据是否已存在
            // 如果已存在，直接返回，保证数据唯一性（不重复插入）
            if (storageService.exists(symbol, period.getPeriod(), windowStart)) {
                log.debug("聚合数据已存在，跳过补齐: symbol={}, period={}, windowStart={}", 
                        symbol, period.getPeriod(), windowStart);
                return false;
            }
            
            // 计算窗口结束时间
            long windowEnd = PeriodCalculator.calculateWindowEnd(windowStart, period);
            
            // 解析symbol-marketType格式，提取标准化的symbol
            // symbol格式：BTC-USDT-SPOT 或 BTC-USDT-SWAP
            // 需要提取：BTC-USDT（用于查询QuestDB）
            String[] parts = symbol.split("-");
            String querySymbol = symbol; // 默认使用原始symbol
            if (parts.length >= 3) {
                // 如果包含marketType后缀，去掉最后一个部分
                // 例如：BTC-USDT-SPOT -> BTC-USDT
                querySymbol = String.join("-", java.util.Arrays.copyOf(parts, parts.length - 1));
            }
            
            // 从QuestDB读取该窗口内的1m数据
            // 注意：QuestDB中存储的symbol可能是标准化的（BTC-USDT），也可能是交易所格式（BTC-USDT-SWAP）
            // 这里先尝试使用标准化symbol查询，如果查询不到，再尝试使用原始symbol
            List<NormalizedKline> klines = marketQueryService.queryKlines(
                    querySymbol, "1m", windowStart, windowEnd, null);
            
            // 如果查询不到，尝试使用原始symbol（可能是交易所格式）
            if (klines.isEmpty() && !querySymbol.equals(symbol)) {
                log.debug("使用标准化symbol查询无结果，尝试使用原始symbol: querySymbol={}, originalSymbol={}", 
                        querySymbol, symbol);
                klines = marketQueryService.queryKlines(
                        symbol, "1m", windowStart, windowEnd, null);
            }
            
            if (klines.isEmpty()) {
                log.debug("窗口内无1m数据，跳过补齐: symbol={}, period={}, windowStart={}", 
                        symbol, period.getPeriod(), windowStart);
                return false;
            }
            
            // 创建AggregationBucket并聚合
            AggregationBucket bucket = new AggregationBucket(symbol, period.getPeriod(), windowStart, windowEnd);
            for (NormalizedKline kline : klines) {
                KlineEvent event = convertToKlineEvent(kline);
                bucket.update(event);
            }
            
            // 生成聚合结果（时间戳必须对齐到窗口起始时间）
            if (bucket.getKlineCount() > 0) {
                // 确保时间戳对齐到周期起始时间
                long alignedTimestamp = PeriodCalculator.alignTimestamp(windowStart, period);
                AggregatedKLine aggregated = bucket.toAggregatedKLine();
                
                // 重新创建AggregatedKLine，确保时间戳对齐
                AggregatedKLine alignedAggregated = new AggregatedKLine(
                        aggregated.symbol(),
                        aggregated.period(),
                        alignedTimestamp,  // 对齐后的时间戳
                        aggregated.open(),
                        aggregated.high(),
                        aggregated.low(),
                        aggregated.close(),
                        aggregated.volume(),
                        aggregated.sourceKlineCount()
                );
                
                // 写入QuestDB（写入前再次检查，双重保证幂等性）
                boolean saved = storageService.save(alignedAggregated);
                if (saved) {
                    log.info("历史数据补齐成功: symbol={}, period={}, timestamp={}, sourceKlineCount={}", 
                            symbol, period.getPeriod(), alignedTimestamp, aggregated.sourceKlineCount());
                    return true;
                } else {
                    log.debug("历史数据补齐跳过（已存在）: symbol={}, period={}, timestamp={}", 
                            symbol, period.getPeriod(), alignedTimestamp);
                    return false;
                }
                
                // 注意：不发布事件（避免重复通知下游）
            } else {
                log.debug("窗口内无有效K线数据，跳过补齐: symbol={}, period={}, windowStart={}", 
                        symbol, period.getPeriod(), windowStart);
                return false;
            }
            
        } catch (Exception e) {
            log.error("补齐窗口历史数据异常: symbol={}, period={}, windowStart={}", 
                    symbol, period.getPeriod(), windowStart, e);
            return false;
        }
    }
    
    /**
     * 将NormalizedKline转换为KlineEvent
     */
    private KlineEvent convertToKlineEvent(NormalizedKline kline) {
        // 假设1m K线的openTime和closeTime
        long openTime = kline.getTimestamp();
        long closeTime = openTime + 60000; // 1分钟
        
        // 转换 long 为 Instant
        java.time.Instant openTimeInstant = com.qyl.v2trade.common.util.TimeUtil.fromEpochMilli(openTime);
        java.time.Instant closeTimeInstant = com.qyl.v2trade.common.util.TimeUtil.fromEpochMilli(closeTime);
        
        return KlineEvent.of(
                kline.getSymbol(),
                "OKX", // 默认交易所
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
}

