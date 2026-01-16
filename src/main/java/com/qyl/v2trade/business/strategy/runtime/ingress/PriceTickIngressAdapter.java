package com.qyl.v2trade.business.strategy.runtime.ingress;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.qyl.v2trade.business.strategy.runtime.trigger.EventKeyBuilder;
import com.qyl.v2trade.business.strategy.runtime.trigger.StrategyTrigger;
import com.qyl.v2trade.business.strategy.runtime.trigger.TriggerType;
import com.qyl.v2trade.business.system.model.entity.TradingPair;
import com.qyl.v2trade.business.system.service.TradingPairService;
import com.qyl.v2trade.market.model.event.PriceTick;
import com.qyl.v2trade.market.subscription.collector.eventbus.PriceEventBus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * 价格事件接入适配器
 * 
 * <p>从价格事件总线接入价格更新事件
 *
 * @author qyl
 */
@Slf4j
@Component
public class PriceTickIngressAdapter {
    
    @Autowired
    private TriggerIngress triggerIngress;
    
    @Autowired(required = false)
    private PriceEventBus priceEventBus;
    
    @Autowired(required = false)
    private TradingPairService tradingPairService;
    
    /**
     * 本地缓存：symbol -> tradingPairId（避免每条事件查库）
     */
    private final Cache<String, Long> symbolToPairIdCache = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(10_000)
        .build();
    
    /**
     * 初始化：订阅价格事件总线
     */
    @PostConstruct
    public void init() {
        if (priceEventBus != null) {
            // 订阅价格事件
            priceEventBus.subscribe(this::onPriceTick);
            log.info("PriceTickIngressAdapter 已订阅价格事件总线");
        } else {
            log.warn("PriceEventBus 未找到，PriceTickIngressAdapter 无法接入");
        }
    }
    
    /**
     * 从价格事件总线接收价格事件
     * @param tick 价格 Tick 事件
     */
    private void onPriceTick(PriceTick tick) {
        try {
            // 1. 根据 symbol 查找 tradingPairId（使用本地缓存）
            Long tradingPairId = getTradingPairId(tick.symbol());
            if (tradingPairId == null) {
                log.debug("无法找到交易对ID，跳过价格事件: symbol={}", tick.symbol());
                return;
            }
            
            // 2. 转换时间戳为 Instant
            Instant priceTimeUtc = Instant.ofEpochMilli(tick.timestamp());
            
            // 3. 生成 eventKey
            String eventKey = EventKeyBuilder.buildPriceKey(
                tradingPairId, 
                tick.timestamp()
            );
            
            // 4. 构建 StrategyTrigger
            StrategyTrigger trigger = new StrategyTrigger();
            trigger.setTriggerType(TriggerType.PRICE);
            trigger.setEventKey(eventKey);
            trigger.setAsOfTimeUtc(priceTimeUtc);
            trigger.setTradingPairId(tradingPairId);
            trigger.setStrategySymbol(tick.symbol());
            trigger.setPrice(tick.price());
            
            // 5. 发送到事件系统
            triggerIngress.accept(trigger);
            
        } catch (Exception e) {
            log.error("处理价格事件失败: symbol={}, price={}, timestamp={}", 
                tick.symbol(), tick.price(), tick.timestamp(), e);
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
}

