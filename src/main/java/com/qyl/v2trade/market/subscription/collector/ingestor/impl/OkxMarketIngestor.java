package com.qyl.v2trade.market.subscription.collector.ingestor.impl;

import com.qyl.v2trade.market.subscription.collector.eventbus.MarketEventBus;
import com.qyl.v2trade.market.subscription.collector.ingestor.MarketIngestor;
import com.qyl.v2trade.market.model.NormalizedKline;
import com.qyl.v2trade.market.model.event.KlineEvent;
import com.qyl.v2trade.market.subscription.collector.websocket.ExchangeWebSocketManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OKX行情采集实现
 * 
 * <p>重构后的实现：
 * <ul>
 *   <li>使用 ExchangeWebSocketManager 管理 WebSocket 连接</li>
 *   <li>订阅 EventBus 接收 KlineEvent</li>
 *   <li>移除回调机制，改为事件驱动</li>
 * </ul>
 *
 * @author qyl
 */
@Slf4j
@Service
public class OkxMarketIngestor implements MarketIngestor {

    @Autowired
    private ExchangeWebSocketManager wsManager;

    @Autowired
    private MarketEventBus eventBus;

    // 交易对ID -> 标准化符号映射
    private final ConcurrentHashMap<Long, String> tradingPairSymbolMap = new ConcurrentHashMap<>();
    
    // 交易对ID -> 交易所符号映射
    private final ConcurrentHashMap<Long, String> tradingPairExchangeSymbolMap = new ConcurrentHashMap<>();

    // EventBus 订阅标志（用于取消订阅）
    private volatile boolean subscribed = false;

    @Override
    public void start() {
        if (subscribed) {
            log.warn("已订阅 EventBus，无需重复启动");
            return;
        }

        // 订阅 KlineEvent
        eventBus.subscribe(this::handleKlineEvent);
        subscribed = true;
        log.info("已订阅 EventBus");

        // 启动 WebSocket 连接
        if (!wsManager.isConnected()) {
            wsManager.connect();
            log.info("行情采集服务已启动，WebSocket连接状态: {}", wsManager.isConnected());
        } else {
            log.info("WebSocket已连接，无需重复启动");
        }
    }
    
    /**
     * 检查WebSocket是否已连接
     */
    public boolean isWebSocketConnected() {
        return wsManager != null && wsManager.isConnected();
    }

    @Override
    public void stop() {
        // 取消 EventBus 订阅
        if (subscribed) {
            // 注意：SimpleMarketEventBus 的 unsubscribe 需要传入相同的 Consumer 实例
            // 这里简化处理，在 v1.0 阶段不实现取消订阅
            subscribed = false;
            log.info("已取消 EventBus 订阅");
        }

        // 取消所有订阅
        Set<String> symbolsToUnsubscribe = new HashSet<>(tradingPairExchangeSymbolMap.values());
        if (!symbolsToUnsubscribe.isEmpty()) {
            wsManager.unsubscribe(symbolsToUnsubscribe);
        }

        tradingPairSymbolMap.clear();
        tradingPairExchangeSymbolMap.clear();
        
        log.info("行情采集服务已停止");
    }

    @Override
    public void subscribe(Long tradingPairId, String symbolOnExchange, String standardSymbol) {
        try {
            // 记录映射关系
            tradingPairSymbolMap.put(tradingPairId, standardSymbol);
            tradingPairExchangeSymbolMap.put(tradingPairId, symbolOnExchange);

            // 订阅 WebSocket（通过 ExchangeWebSocketManager）
            Set<String> symbols = new HashSet<>();
            symbols.add(symbolOnExchange);
            wsManager.subscribe(symbols);

            log.info("订阅行情成功: tradingPairId={}, symbolOnExchange={}, standardSymbol={}", 
                    tradingPairId, symbolOnExchange, standardSymbol);
        } catch (Exception e) {
            log.error("订阅行情失败: tradingPairId={}, symbolOnExchange={}", tradingPairId, symbolOnExchange, e);
            throw new RuntimeException("订阅失败", e);
        }
    }

    @Override
    public void unsubscribe(Long tradingPairId) {
        String symbolOnExchange = tradingPairExchangeSymbolMap.remove(tradingPairId);
        tradingPairSymbolMap.remove(tradingPairId);

        if (symbolOnExchange != null) {
            try {
                Set<String> symbols = new HashSet<>();
                symbols.add(symbolOnExchange);
                wsManager.unsubscribe(symbols);
                log.info("取消订阅成功: tradingPairId={}, symbolOnExchange={}", tradingPairId, symbolOnExchange);
            } catch (Exception e) {
                log.error("取消订阅失败: tradingPairId={}, symbolOnExchange={}", tradingPairId, symbolOnExchange, e);
            }
        }
    }

    /**
     * 处理 KlineEvent（从 EventBus 接收）
     * 
     * <p>将 KlineEvent 转换为 NormalizedKline，保持向后兼容。
     * 注意：这里只是转换，实际处理逻辑在 MarketDataCenter 中。
     */
    private void handleKlineEvent(KlineEvent event) {
        try {
            log.debug("收到 KlineEvent: symbol={}, timestamp={}, close={}", 
                    event.symbol(), event.openTime(), event.close());

            // 转换为 NormalizedKline（保持向后兼容）
            // 注意：这里只是记录日志，实际处理由 MarketDataCenter 负责
            // 因为 MarketDataCenter 也会订阅 EventBus
            
        } catch (Exception e) {
            log.error("处理 KlineEvent 失败: symbol={}, timestamp={}", 
                    event.symbol(), event.openTime(), e);
        }
    }

    /**
     * 将 KlineEvent 转换为 NormalizedKline
     * 
     * <p>用于向后兼容，将新的事件模型转换为旧的模型。
     * 
     * @param event KlineEvent
     * @return NormalizedKline
     */
    public static NormalizedKline convertToNormalizedKline(KlineEvent event) {
        return NormalizedKline.builder()
                .symbol(event.symbol())
                .interval(event.interval())
                .open(event.open().doubleValue())
                .high(event.high().doubleValue())
                .low(event.low().doubleValue())
                .close(event.close().doubleValue())
                .volume(event.volume().doubleValue())
                .timestamp(event.openTime())
                .exchangeTimestamp(event.openTime())
                .build();
    }
}
