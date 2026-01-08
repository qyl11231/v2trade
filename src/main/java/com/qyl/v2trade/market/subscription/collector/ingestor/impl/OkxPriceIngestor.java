package com.qyl.v2trade.market.subscription.collector.ingestor.impl;

import com.qyl.v2trade.market.subscription.collector.ingestor.PriceIngestor;
import com.qyl.v2trade.market.subscription.collector.websocket.PriceWebSocketManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OKX价格订阅采集实现
 * 
 * <p>使用 PriceWebSocketManager 管理价格订阅WebSocket连接（/ws/v5/public端点）
 *
 * @author qyl
 */
@Slf4j
@Service
public class OkxPriceIngestor implements PriceIngestor {

    @Autowired
    private PriceWebSocketManager priceWsManager;

    // 价格订阅：交易对ID -> 交易所符号映射
    private final ConcurrentHashMap<Long, String> priceSubscriptionMap = new ConcurrentHashMap<>();

    @Override
    public void start() {
        // 启动 WebSocket 连接（如果未连接）
        if (!priceWsManager.isConnected()) {
            priceWsManager.connect();
            log.info("价格订阅服务已启动，WebSocket连接状态: {}", priceWsManager.isConnected());
        } else {
            log.info("价格订阅WebSocket已连接，无需重复启动");
        }
    }

    @Override
    public void stop() {
        // 取消所有订阅
        Set<String> symbolsToUnsubscribe = new HashSet<>(priceSubscriptionMap.values());
        if (!symbolsToUnsubscribe.isEmpty()) {
            priceWsManager.unsubscribePrice(symbolsToUnsubscribe);
        }

        priceSubscriptionMap.clear();
        
        log.info("价格订阅服务已停止");
    }

    @Override
    public void subscribe(Long tradingPairId, String symbolOnExchange, String standardSymbol) {
        try {
            // 记录映射关系
            priceSubscriptionMap.put(tradingPairId, symbolOnExchange);

            // 订阅价格（通过 PriceWebSocketManager）
            Set<String> symbols = new HashSet<>();
            symbols.add(symbolOnExchange);
            priceWsManager.subscribePrice(symbols);

            log.info("订阅价格成功: tradingPairId={}, symbolOnExchange={}, standardSymbol={}", 
                    tradingPairId, symbolOnExchange, standardSymbol);
        } catch (Exception e) {
            log.error("订阅价格失败: tradingPairId={}, symbolOnExchange={}", tradingPairId, symbolOnExchange, e);
            throw new RuntimeException("订阅价格失败", e);
        }
    }

    @Override
    public void unsubscribe(Long tradingPairId) {
        String symbolOnExchange = priceSubscriptionMap.remove(tradingPairId);

        if (symbolOnExchange != null) {
            try {
                Set<String> symbols = new HashSet<>();
                symbols.add(symbolOnExchange);
                priceWsManager.unsubscribePrice(symbols);
                log.info("取消订阅价格成功: tradingPairId={}, symbolOnExchange={}", tradingPairId, symbolOnExchange);
            } catch (Exception e) {
                log.error("取消订阅价格失败: tradingPairId={}, symbolOnExchange={}", tradingPairId, symbolOnExchange, e);
            }
        }
    }
}

