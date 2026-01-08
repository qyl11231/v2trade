package com.qyl.v2trade.market.model.event;

import java.math.BigDecimal;

/**
 * 价格变更事件（对外发布）
 * 
 * <p>用于向系统其他模块发布价格变更通知。
 * 订阅者：策略模块、风控模块、前端展示等。
 *
 * @author qyl
 */
public record PriceChangedEvent(
    /**
     * 交易对符号
     */
    String symbol,
    
    /**
     * 最新价格
     */
    BigDecimal price,
    
    /**
     * 时间戳（毫秒）
     */
    long timestamp
) {
    /**
     * 创建 PriceChangedEvent 实例
     * 
     * @param symbol 交易对符号
     * @param price 最新价格
     * @param timestamp 时间戳（毫秒）
     * @return PriceChangedEvent 实例
     */
    public static PriceChangedEvent of(String symbol, BigDecimal price, long timestamp) {
        return new PriceChangedEvent(symbol, price, timestamp);
    }
}

