package com.qyl.v2trade.market.subscription.collector.channel;

import java.util.Set;

/**
 * 行情频道接口
 * 
 * <p>所有行情类型（K线、深度、成交等）必须实现此接口。
 * 通过该接口实现统一的频道管理机制，支持动态扩展新的行情类型。
 *
 * @author qyl
 */
public interface MarketChannel {

    /**
     * 频道类型常量
     */
    String CHANNEL_TYPE_KLINE = "KLINE";
    String CHANNEL_TYPE_TICKER = "TICKER";
    String CHANNEL_TYPE_TRADE = "TRADE";
    String CHANNEL_TYPE_ORDERBOOK = "ORDERBOOK";

    /**
     * 获取频道类型
     * 
     * @return 频道类型（如：KLINE, TICKER, TRADE）
     */
    String channelType();

    /**
     * 构建订阅消息
     * 
     * @param symbols 交易对符号集合（如：["BTC-USDT-SWAP", "ETH-USDT-SWAP"]）
     * @return 订阅消息的 JSON 字符串
     */
    String buildSubscribeRaw(Set<String> symbols);

    /**
     * 构建取消订阅消息
     * 
     * @param symbols 交易对符号集合
     * @return 取消订阅消息的 JSON 字符串
     */
    String buildUnsubscribeRaw(Set<String> symbols);

    /**
     * 处理 WebSocket 消息
     * 
     * <p>将原始 JSON 消息解析为标准化事件并发布到 EventBus。
     * 
     * @param rawJson 原始 JSON 消息字符串
     */
    void onMessage(String rawJson);

    /**
     * 检查频道是否就绪
     * 
     * <p>订阅是一个过程（发送 Sub -> 收到 Ack），系统需要知道某个频道是否真的"订阅成功"。
     * 
     * @return true 表示频道已就绪，可以接收数据
     */
    default boolean isReady() {
        return true;
    }
}

