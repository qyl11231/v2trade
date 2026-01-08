package com.qyl.v2trade.market.model.event;

import java.math.BigDecimal;

/**
 * 实时价格Tick事件（内部使用）
 * 
 * <p>用于在订阅模块内部传递价格数据，从WebSocket消息解析而来。
 * 不对外暴露，仅用于模块内部流转。
 *
 * @author qyl
 */
public record PriceTick(
    /**
     * 交易对符号（交易所格式，如：BTC-USDT-SWAP）
     */
    String symbol,
    
    /**
     * 当前最新成交价
     */
    BigDecimal price,
    
    /**
     * 交易所时间戳（毫秒）
     */
    long timestamp,
    
    /**
     * 行情来源（如：OKX）
     */
    String source
) {
    /**
     * 创建 PriceTick 实例
     * 
     * @param symbol 交易对符号
     * @param price 最新成交价
     * @param timestamp 交易所时间戳（毫秒）
     * @param source 行情来源
     * @return PriceTick 实例
     */
    public static PriceTick of(String symbol, BigDecimal price, long timestamp, String source) {
        return new PriceTick(symbol, price, timestamp, source);
    }
}

