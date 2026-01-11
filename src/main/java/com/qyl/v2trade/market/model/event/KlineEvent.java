package com.qyl.v2trade.market.model.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 标准化 K 线事件 - 跨交易所通用
 * 
 * <p>用于在行情订阅模块内部传递标准化的 K 线数据，屏蔽不同交易所协议的差异。
 * 
 * <p>注意：
 * <ul>
 *   <li>isFinal 字段在 v1.0 阶段默认设为 false，由下游 MarketDataCenter 负责判断</li>
 *   <li>eventTime 是消息到达本系统的时间（UTC），用于延迟监控</li>
 *   <li>所有时间字段统一使用 {@link Instant} 类型，表示 UTC 时间点</li>
 * </ul>
 *
 * @author qyl
 */
public record KlineEvent(
    /**
     * 交易对符号（标准化格式，如：BTC-USDT）
     */
    String symbol,
    
    /**
     * 交易所名称（如：OKX, Binance）
     */
    String exchange,
    
    /**
     * K 线开盘时间（UTC）
     */
    Instant openTime,
    
    /**
     * K 线收盘时间（UTC）
     */
    Instant closeTime,
    
    /**
     * K 线周期（如：1m, 5m, 1h）
     */
    String interval,
    
    /**
     * 开盘价
     */
    BigDecimal open,
    
    /**
     * 最高价
     */
    BigDecimal high,
    
    /**
     * 最低价
     */
    BigDecimal low,
    
    /**
     * 收盘价
     */
    BigDecimal close,
    
    /**
     * 成交量
     */
    BigDecimal volume,
    
    /**
     * 是否为该周期最终值
     * 
     * <p>由于 WebSocket 是实时推送，未完结的 K 线会持续更新。
     * v1.0 阶段默认设为 false，由下游 MarketDataCenter 通过时间戳比较判断。
     */
    boolean isFinal,
    
    /**
     * 消息到达本系统的时间（UTC）
     * 
     * <p>用于延迟监控，计算从交易所发出到本系统处理完成的延迟。
     */
    Instant eventTime
) {
    /**
     * 创建 KlineEvent 实例
     * 
     * @param symbol 交易对符号
     * @param exchange 交易所名称
     * @param openTime 开盘时间（UTC）
     * @param closeTime 收盘时间（UTC）
     * @param interval 周期
     * @param open 开盘价
     * @param high 最高价
     * @param low 最低价
     * @param close 收盘价
     * @param volume 成交量
     * @param isFinal 是否最终值
     * @param eventTime 事件时间（UTC）
     * @return KlineEvent 实例
     */
    public static KlineEvent of(
            String symbol,
            String exchange,
            Instant openTime,
            Instant closeTime,
            String interval,
            BigDecimal open,
            BigDecimal high,
            BigDecimal low,
            BigDecimal close,
            BigDecimal volume,
            boolean isFinal,
            Instant eventTime) {
        return new KlineEvent(
                symbol,
                exchange,
                openTime,
                closeTime,
                interval,
                open,
                high,
                low,
                close,
                volume,
                isFinal,
                eventTime
        );
    }
}

