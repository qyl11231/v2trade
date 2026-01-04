package com.qyl.v2trade.market.aggregation.core;

import com.qyl.v2trade.market.aggregation.config.SupportedPeriod;

/**
 * 周期计算工具类
 * 
 * <p>用于计算时间窗口的起始和结束时间，确保所有时间戳对齐到对应周期的起始时间。
 * 
 * <p>核心原则：所有时间戳必须对齐到对应周期的起始时间
 *
 * @author qyl
 */
public class PeriodCalculator {
    
    /**
     * 计算时间戳所属的聚合窗口起始时间
     * 
     * <p>【重要】时间对齐规则：
     * <ul>
     *   <li>所有聚合结果的时间戳必须对齐到对应周期的起始时间</li>
     *   <li>例如：5m周期，10:03的1m K线 -> 对齐到10:00</li>
     *   <li>例如：15m周期，10:20的1m K线 -> 对齐到10:15</li>
     *   <li>例如：1h周期，10:45的1m K线 -> 对齐到10:00</li>
     * </ul>
     * 
     * @param timestamp 1m K线时间戳（已对齐到分钟，毫秒）
     * @param period 周期
     * @return 窗口起始时间戳（已对齐到周期边界，毫秒）
     */
    public static long calculateWindowStart(long timestamp, SupportedPeriod period) {
        long durationMs = period.getDurationMs();
        // 对齐到周期边界（向下取整）
        return (timestamp / durationMs) * durationMs;
    }
    
    /**
     * 计算窗口结束时间
     * 
     * @param windowStart 窗口起始时间戳（毫秒）
     * @param period 周期
     * @return 窗口结束时间戳（毫秒）
     */
    public static long calculateWindowEnd(long windowStart, SupportedPeriod period) {
        return windowStart + period.getDurationMs();
    }
    
    /**
     * 对齐时间戳到周期起始时间
     * 
     * <p>【重要】所有聚合K线的时间戳必须使用此方法对齐
     * 
     * @param timestamp 原始时间戳（毫秒）
     * @param period 周期
     * @return 对齐后的时间戳（周期起始时间，毫秒）
     */
    public static long alignTimestamp(long timestamp, SupportedPeriod period) {
        return calculateWindowStart(timestamp, period);
    }
}

