package com.qyl.v2trade.market.subscription.service;

import java.math.BigDecimal;

/**
 * 最新价格状态（不可变）
 * 
 * <p>用于在内存中维护每个交易对的最新价格状态。
 *
 * @author qyl
 */
public record LatestPrice(
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
     * 创建 LatestPrice 实例
     * 
     * @param symbol 交易对符号
     * @param price 最新价格
     * @param timestamp 时间戳（毫秒）
     * @return LatestPrice 实例
     */
    public static LatestPrice of(String symbol, BigDecimal price, long timestamp) {
        return new LatestPrice(symbol, price, timestamp);
    }
}

