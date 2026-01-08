package com.qyl.v2trade.market.subscription.collector.channel.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qyl.v2trade.market.subscription.collector.channel.MarketChannel;
import com.qyl.v2trade.market.subscription.collector.eventbus.PriceEventBus;
import com.qyl.v2trade.market.model.event.PriceTick;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 价格频道实现
 * 
 * <p>负责解析 OKX ticker 消息并转换为标准化的 PriceTick 事件。
 * 
 * <p>OKX Ticker 消息格式：
 * <pre>
 * {
 *   "arg": {
 *     "channel": "tickers",  // 注意：合约使用"tickers"（复数），现货使用"ticker"（单数）
 *     "instId": "BTC-USDT-SWAP"
 *   },
 *   "data": [{
 *     "instId": "BTC-USDT-SWAP",
 *     "last": "42000.5",
 *     "ts": "1710000000000"
 *   }]
 * }
 * </pre>
 * 
 * <p>注意：
 * <ul>
 *   <li>现货（SPOT）使用 "ticker" 频道，连接到 /ws/v5/public 端点</li>
 *   <li>合约（SWAP/FUTURES）使用 "tickers" 频道，连接到 /ws/v5/business 端点</li>
 * </ul>
 *
 * @author qyl
 */
@Slf4j
@Component
public class PriceChannel implements MarketChannel {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PriceEventBus priceEventBus;

    @Override
    public String channelType() {
        return CHANNEL_TYPE_PRICE;
    }

    @Override
    public String buildSubscribeRaw(Set<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            log.warn("订阅符号集合为空");
            return null;
        }

        // OKX 订阅消息格式：
        // 合约使用: {"op":"subscribe","args":[{"channel":"tickers","instId":"BTC-USDT-SWAP"}]}
        // 现货使用: {"op":"subscribe","args":[{"channel":"ticker","instId":"BTC-USDT"}]}
        // 注意：实际订阅由 PriceWebSocketManager 发送，这里仅作参考
        String args = symbols.stream()
                .map(symbol -> String.format("{\"channel\":\"tickers\",\"instId\":\"%s\"}", symbol))
                .collect(Collectors.joining(","));

        return String.format("{\"op\":\"subscribe\",\"args\":[%s]}", args);
    }

    @Override
    public String buildUnsubscribeRaw(Set<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            log.warn("取消订阅符号集合为空");
            return null;
        }

        // OKX 取消订阅消息格式：
        // 合约使用: {"op":"unsubscribe","args":[{"channel":"tickers","instId":"BTC-USDT-SWAP"}]}
        // 现货使用: {"op":"unsubscribe","args":[{"channel":"ticker","instId":"BTC-USDT"}]}
        // 注意：实际取消订阅由 PriceWebSocketManager 发送，这里仅作参考
        String args = symbols.stream()
                .map(symbol -> String.format("{\"channel\":\"tickers\",\"instId\":\"%s\"}", symbol))
                .collect(Collectors.joining(","));

        return String.format("{\"op\":\"unsubscribe\",\"args\":[%s]}", args);
    }

    @Override
    public void onMessage(String rawJson) {
        log.debug("PriceChannel收到消息: messageLength={}", rawJson != null ? rawJson.length() : 0);
        try {
            JsonNode root = objectMapper.readTree(rawJson);

            // 检查是否有 data 字段
            JsonNode dataNode = root.path("data");
            if (!dataNode.isArray() || dataNode.size() == 0) {
                log.debug("OKX ticker消息中没有数据: {}", rawJson);
                return;
            }

            // 获取 arg 信息
            JsonNode argNode = root.path("arg");
            if (!argNode.has("instId")) {
                log.warn("OKX ticker消息缺少 instId: {}", rawJson);
                return;
            }

            String instId = argNode.get("instId").asText();

            // 处理第一个数据项（ticker通常只有一个数据项）
            JsonNode dataItem = dataNode.get(0);
            if (dataItem == null || !dataItem.isObject()) {
                log.warn("OKX ticker数据格式异常: {}", rawJson);
                return;
            }

            // 提取价格字段
            if (!dataItem.has("last")) {
                log.warn("OKX ticker消息缺少 last 字段: {}", rawJson);
                return;
            }

            String lastPriceStr = dataItem.get("last").asText();
            BigDecimal price;
            try {
                price = new BigDecimal(lastPriceStr);
            } catch (NumberFormatException e) {
                log.warn("OKX ticker价格格式异常: last={}, message={}", lastPriceStr, rawJson, e);
                return;
            }

            // 提取时间戳字段
            long timestamp;
            if (dataItem.has("ts")) {
                String tsStr = dataItem.get("ts").asText();
                try {
                    timestamp = Long.parseLong(tsStr);
                } catch (NumberFormatException e) {
                    log.warn("OKX ticker时间戳格式异常: ts={}, message={}", tsStr, rawJson, e);
                    return;
                }
            } else {
                // 如果没有ts字段，使用当前时间（但不推荐，应该记录警告）
                timestamp = System.currentTimeMillis();
                log.warn("OKX ticker消息缺少 ts 字段，使用当前时间: {}", rawJson);
            }

            // 创建 PriceTick 事件
            PriceTick tick = PriceTick.of(instId, price, timestamp, "OKX");

            // 发布到 PriceEventBus
            priceEventBus.publish(tick);

        } catch (Exception e) {
            log.error("处理 OKX ticker 消息失败: {}", rawJson, e);
            // 异常隔离：单个消息异常不影响其他消息
        }
    }
}

