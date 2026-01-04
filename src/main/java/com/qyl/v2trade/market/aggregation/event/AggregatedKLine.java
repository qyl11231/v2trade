package com.qyl.v2trade.market.aggregation.event;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 聚合K线事件（不可变、可序列化）
 * 
 * <p>语义：一根聚合完成的K线，时间窗口已关闭
 * 
 * <p>所有多周期K线都从1m K线聚合生成，该事件表示一个聚合窗口的完整结果。
 *
 * @author qyl
 */
public record AggregatedKLine(
    /**
     * 交易对符号（如：BTC-USDT）
     */
    String symbol,
    
    /**
     * K线周期（如：5m, 15m, 1h）
     */
    String period,
    
    /**
     * 时间戳（毫秒，UTC，必须对齐到周期起始点）
     * 
     * <p>【重要】时间对齐规则：
     * <ul>
     *   <li>5m周期：必须对齐到5分钟边界（如：10:00, 10:05, 10:10）</li>
     *   <li>15m周期：必须对齐到15分钟边界（如：10:00, 10:15, 10:30）</li>
     *   <li>30m周期：必须对齐到30分钟边界（如：10:00, 10:30）</li>
     *   <li>1h周期：必须对齐到小时边界（如：10:00, 11:00）</li>
     *   <li>4h周期：必须对齐到4小时边界（如：00:00, 04:00, 08:00）</li>
     * </ul>
     * 
     * <p>使用 PeriodCalculator.alignTimestamp() 确保对齐
     */
    long timestamp,
    
    /**
     * 开盘价（窗口内第一根1m K线的开盘价）
     */
    BigDecimal open,
    
    /**
     * 最高价（窗口内所有1m K线的最高价）
     */
    BigDecimal high,
    
    /**
     * 最低价（窗口内所有1m K线的最低价）
     */
    BigDecimal low,
    
    /**
     * 收盘价（窗口内最后一根1m K线的收盘价）
     */
    BigDecimal close,
    
    /**
     * 成交量（窗口内所有1m K线的成交量之和）
     */
    BigDecimal volume,
    
    /**
     * 聚合的1m K线数量（用于验证完整性）
     */
    int sourceKlineCount
) implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * 创建聚合K线事件
     * 
     * @param symbol 交易对符号
     * @param period 周期
     * @param timestamp 时间戳（已对齐到周期起始点）
     * @param open 开盘价
     * @param high 最高价
     * @param low 最低价
     * @param close 收盘价
     * @param volume 成交量
     * @param sourceKlineCount 聚合的1m K线数量
     * @return AggregatedKLine 实例
     */
    public static AggregatedKLine of(
            String symbol,
            String period,
            long timestamp,
            BigDecimal open,
            BigDecimal high,
            BigDecimal low,
            BigDecimal close,
            BigDecimal volume,
            int sourceKlineCount) {
        return new AggregatedKLine(
                symbol, period, timestamp,
                open, high, low, close, volume,
                sourceKlineCount
        );
    }
}

