package com.qyl.v2trade.business.strategy.runtime.ingress;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.qyl.v2trade.business.strategy.runtime.trigger.EventKeyBuilder;
import com.qyl.v2trade.business.strategy.runtime.trigger.StrategyTrigger;
import com.qyl.v2trade.business.strategy.runtime.trigger.TriggerType;
import com.qyl.v2trade.business.system.model.entity.TradingPair;
import com.qyl.v2trade.business.system.service.TradingPairService;
import com.qyl.v2trade.market.aggregation.core.KlineAggregator;
import com.qyl.v2trade.market.aggregation.event.AggregatedKLine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * K线闭合事件接入适配器
 * 
 * <p>从 K 线聚合器接入 bar close 事件
 *
 * @author qyl
 */
@Slf4j
@Component
public class BarClosedIngressAdapter {
    
    @Autowired
    private TriggerIngress triggerIngress;
    
    @Autowired(required = false)
    private TradingPairService tradingPairService;
    
    @Autowired(required = false)
    private KlineAggregator klineAggregator;
    
    /**
     * 本地缓存：symbol -> tradingPairId（避免每条事件查库）
     */
    private final Cache<String, Long> symbolToPairIdCache = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(10_000)
        .build();
    
    /**
     * 初始化：订阅 K 线聚合器的回调
     */
    @PostConstruct
    public void init() {
        if (klineAggregator != null) {
            // 设置聚合完成回调，当 K 线窗口闭合时触发
            if (klineAggregator instanceof com.qyl.v2trade.market.aggregation.core.impl.KlineAggregatorImpl) {
                ((com.qyl.v2trade.market.aggregation.core.impl.KlineAggregatorImpl) klineAggregator)
                    .setAggregationCallback(this::onBarClosed);
                log.info("BarClosedIngressAdapter 已订阅 K 线聚合器回调");
            } else {
                log.warn("KlineAggregator 类型不匹配，无法设置回调");
            }
        } else {
            log.warn("KlineAggregator 未找到，BarClosedIngressAdapter 无法接入");
        }
    }
    
    /**
     * 从 K 线聚合器接收 bar close 事件
     * @param aggregatedKLine 聚合完成的 K 线
     */
    private void onBarClosed(AggregatedKLine aggregatedKLine) {
        try {
            // 1. 根据 symbol 查找 tradingPairId（使用本地缓存）
            Long tradingPairId = getTradingPairId(aggregatedKLine.symbol());
            if (tradingPairId == null) {
                log.warn("无法找到交易对ID，跳过事件: symbol={}", aggregatedKLine.symbol());
                return;
            }
            
            // 2. 计算 K 线闭合时间（timestamp 是窗口起始时间，闭合时间是窗口结束时间）
            // 根据 period 计算窗口时长
            long windowDurationMs = calculateWindowDurationMs(aggregatedKLine.period());
            long barCloseTimeMillis = aggregatedKLine.timestamp() + windowDurationMs;
            Instant barCloseTimeUtc = Instant.ofEpochMilli(barCloseTimeMillis);
            
            // 3. 生成 eventKey
            String eventKey = EventKeyBuilder.buildBarCloseKey(
                tradingPairId, 
                aggregatedKLine.period(), 
                barCloseTimeMillis
            );
            
            // 4. 构建 StrategyTrigger（包含完整的 OHLC 数据）
            StrategyTrigger trigger = new StrategyTrigger();
            trigger.setTriggerType(TriggerType.BAR_CLOSE);
            trigger.setEventKey(eventKey);
            trigger.setAsOfTimeUtc(barCloseTimeUtc);
            trigger.setTradingPairId(tradingPairId);
            trigger.setStrategySymbol(aggregatedKLine.symbol());
            trigger.setTimeframe(aggregatedKLine.period());
            // 设置 K 线 OHLC 数据
            trigger.setBarOpen(aggregatedKLine.open());
            trigger.setBarHigh(aggregatedKLine.high());
            trigger.setBarLow(aggregatedKLine.low());
            trigger.setBarClose(aggregatedKLine.close());
            trigger.setBarVolume(aggregatedKLine.volume());
            
            // 5. 发送到事件系统
            triggerIngress.accept(trigger);
            
        } catch (Exception e) {
            log.error("处理 K 线闭合事件失败: symbol={}, period={}, timestamp={}", 
                aggregatedKLine.symbol(), aggregatedKLine.period(), aggregatedKLine.timestamp(), e);
        }
    }
    
    /**
     * 根据 symbol 获取 tradingPairId（使用本地缓存）
     */
    private Long getTradingPairId(String symbol) {
        // 先从缓存获取
        Long cached = symbolToPairIdCache.getIfPresent(symbol);
        if (cached != null) {
            return cached;
        }
        
        if (tradingPairService == null) {
            return null;
        }
        
        try {
            // 先尝试 SWAP，再尝试 SPOT
            TradingPair pair = tradingPairService.getBySymbolAndMarketType(symbol, "SWAP");
            if (pair == null) {
                pair = tradingPairService.getBySymbolAndMarketType(symbol, "SPOT");
            }
            
            Long pairId = pair != null ? pair.getId() : null;
            if (pairId != null) {
                // 缓存结果
                symbolToPairIdCache.put(symbol, pairId);
            }
            return pairId;
        } catch (Exception e) {
            log.warn("获取 tradingPairId 失败: symbol={}", symbol, e);
            return null;
        }
    }
    
    /**
     * 计算窗口时长（毫秒）
     */
    private long calculateWindowDurationMs(String period) {
        return switch (period) {
            case "1m" -> 60_000L;
            case "5m" -> 300_000L;
            case "15m" -> 900_000L;
            case "30m" -> 1_800_000L;
            case "1h" -> 3_600_000L;
            case "4h" -> 14_400_000L;
            case "1d" -> 86_400_000L;
            default -> {
                log.warn("未知周期，使用默认1分钟: period={}", period);
                yield 60_000L;
            }
        };
    }
    
    /**
     * 测试方法：手动触发 K 线闭合事件
     * 
     * @param tradingPairId 交易对ID
     * @param timeframe 时间周期（如 "5m", "1h"）
     * @param barCloseTimeMillis K线闭合时间戳（毫秒，UTC）
     * @param strategySymbol 策略交易对符号（可选）
     */
    public void testBarClose(Long tradingPairId, String timeframe, long barCloseTimeMillis, String strategySymbol) {
        try {
            Instant barCloseTimeUtc = Instant.ofEpochMilli(barCloseTimeMillis);
            
            // 生成 eventKey
            String eventKey = EventKeyBuilder.buildBarCloseKey(
                tradingPairId, 
                timeframe, 
                barCloseTimeMillis
            );
            
            // 构建 StrategyTrigger
            StrategyTrigger trigger = new StrategyTrigger();
            trigger.setTriggerType(TriggerType.BAR_CLOSE);
            trigger.setEventKey(eventKey);
            trigger.setAsOfTimeUtc(barCloseTimeUtc);
            trigger.setTradingPairId(tradingPairId);
            trigger.setStrategySymbol(strategySymbol);
            trigger.setTimeframe(timeframe);
            
            // 发送到事件系统
            triggerIngress.accept(trigger);
            
            log.info("测试 K 线闭合事件已发送: tradingPairId={}, timeframe={}, barCloseTime={}", 
                tradingPairId, timeframe, barCloseTimeUtc);
        } catch (Exception e) {
            log.error("测试 K 线闭合事件失败: tradingPairId={}, timeframe={}", tradingPairId, timeframe, e);
            throw new RuntimeException("测试 K 线闭合事件失败", e);
        }
    }
}

