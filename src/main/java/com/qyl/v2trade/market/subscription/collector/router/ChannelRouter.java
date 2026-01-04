package com.qyl.v2trade.market.subscription.collector.router;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qyl.v2trade.market.subscription.collector.channel.MarketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * 消息路由器
 * 
 * <p>负责从 WebSocket 消息中识别频道类型，并路由到对应的 MarketChannel。
 * 
 * <p>关键功能：
 * <ul>
 *   <li>系统消息过滤：过滤 subscribe/error/login 等非行情消息</li>
 *   <li>频道识别：从消息中提取 channel 类型并路由</li>
 *   <li>异常隔离：单个 Channel 异常不影响其他 Channel</li>
 * </ul>
 *
 * @author qyl
 */
@Slf4j
public class ChannelRouter {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Channel 注册表（channelType -> MarketChannel）
     */
    private final Map<String, MarketChannel> channels = new HashMap<>();

    /**
     * 注册 Channel
     * 
     * @param channel MarketChannel 实例
     */
    public void registerChannel(MarketChannel channel) {
        if (channel == null) {
            log.warn("尝试注册 null Channel，跳过");
            return;
        }

        String channelType = channel.channelType();
        channels.put(channelType, channel);
        log.info("注册 Channel: type={}", channelType);
    }

    /**
     * 路由消息
     * 
     * <p>处理流程：
     * <ol>
     *   <li>解析 JSON 消息</li>
     *   <li>检查是否为系统消息（event 字段），如果是则跳过</li>
     *   <li>提取 channel 类型</li>
     *   <li>路由到对应的 MarketChannel</li>
     * </ol>
     * 
     * @param rawMessage 原始 JSON 消息
     */
    public void route(String rawMessage) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            log.debug("收到空消息，跳过");
            return;
        }

        try {
            // 解析 JSON（使用 JsonNode 避免二次解析）
            JsonNode root = objectMapper.readTree(rawMessage);

            // 关键：过滤系统消息（subscribe/error/login 响应）
            if (isSystemMessage(root)) {
                log.debug("系统消息，跳过路由: {}", rawMessage);
                return;
            }

            // 提取 channel 类型
            String channelType = extractChannelType(root);
            if (channelType == null) {
                log.debug("无法识别频道类型，跳过: {}", rawMessage);
                return;
            }

            // 路由到对应的 Channel
            MarketChannel channel = channels.get(channelType);
            if (channel == null) {
                log.warn("未找到对应的 Channel: channelType={}, message={}", channelType, rawMessage);
                return;
            }

            // 调用 Channel 的 onMessage（异常隔离）
            try {
                channel.onMessage(rawMessage);
            } catch (Exception e) {
                log.error("Channel 处理消息异常: channelType={}", channelType, e);
                // 异常隔离：单个 Channel 异常不影响其他 Channel
            }

        } catch (Exception e) {
            log.error("路由消息失败: {}", rawMessage, e);
        }
    }

    /**
     * 判断是否为系统消息
     * 
     * <p>OKX 系统消息格式：
     * <pre>
     * {"event":"subscribe", "arg":{...}}
     * {"event":"error", "msg":"..."}
     * {"event":"login", "code":"0"}
     * </pre>
     * 
     * @param root JSON 根节点
     * @return true 表示是系统消息，应该跳过
     */
    private boolean isSystemMessage(JsonNode root) {
        if (!root.has("event")) {
            return false;
        }

        String event = root.get("event").asText();
        // 系统消息类型：subscribe, unsubscribe, error, login
        return "subscribe".equals(event) || 
               "unsubscribe".equals(event) || 
               "error".equals(event) || 
               "login".equals(event);
    }

    /**
     * 提取频道类型
     * 
     * <p>从 OKX 消息中提取 channel 类型：
     * <pre>
     * {"arg": {"channel": "candle1m", "instId": "BTC-USDT-SWAP"}, "data": [...]}
     * </pre>
     * 
     * <p>根据 channel 值判断频道类型：
     * <ul>
     *   <li>candle* -> KLINE</li>
     *   <li>ticker -> TICKER</li>
     *   <li>trades -> TRADE</li>
     *   <li>books* -> ORDERBOOK</li>
     * </ul>
     * 
     * @param root JSON 根节点
     * @return 频道类型（如：KLINE），如果无法识别则返回 null
     */
    private String extractChannelType(JsonNode root) {
        JsonNode argNode = root.path("arg");
        if (!argNode.has("channel")) {
            return null;
        }

        String channel = argNode.get("channel").asText();

        // 根据 channel 前缀判断频道类型
        if (channel.startsWith("candle")) {
            return MarketChannel.CHANNEL_TYPE_KLINE;
        } else if ("ticker".equals(channel)) {
            return MarketChannel.CHANNEL_TYPE_TICKER;
        } else if ("trades".equals(channel)) {
            return MarketChannel.CHANNEL_TYPE_TRADE;
        } else if (channel.startsWith("books")) {
            return MarketChannel.CHANNEL_TYPE_ORDERBOOK;
        }

        return null;
    }

    /**
     * 获取已注册的 Channel 数量
     * 
     * @return Channel 数量
     */
    public int getChannelCount() {
        return channels.size();
    }
}

