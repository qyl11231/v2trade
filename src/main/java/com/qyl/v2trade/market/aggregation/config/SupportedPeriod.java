package com.qyl.v2trade.market.aggregation.config;

/**
 * 支持的聚合周期
 * 
 * <p>定义系统支持的K线聚合周期，所有周期都从1m K线聚合生成。
 *
 * @author qyl
 */
public enum SupportedPeriod {
    
    /** 5分钟周期 */
    M5("5m", 5 * 60 * 1000L),
    
    /** 15分钟周期 */
    M15("15m", 15 * 60 * 1000L),
    
    /** 30分钟周期 */
    M30("30m", 30 * 60 * 1000L),
    
    /** 1小时周期 */
    H1("1h", 60 * 60 * 1000L),
    
    /** 4小时周期 */
    H4("4h", 4 * 60 * 60 * 1000L);
    
    /**
     * 周期字符串表示（如：5m, 15m, 1h）
     */
    private final String period;
    
    /**
     * 周期持续时间（毫秒）
     */
    private final long durationMs;
    
    SupportedPeriod(String period, long durationMs) {
        this.period = period;
        this.durationMs = durationMs;
    }
    
    /**
     * 获取周期字符串表示
     * 
     * @return 周期字符串（如：5m, 15m, 1h）
     */
    public String getPeriod() {
        return period;
    }
    
    /**
     * 获取周期持续时间（毫秒）
     * 
     * @return 周期持续时间（毫秒）
     */
    public long getDurationMs() {
        return durationMs;
    }
    
    /**
     * 根据周期字符串查找对应的枚举值
     * 
     * @param period 周期字符串（如：5m, 15m, 1h）
     * @return 对应的枚举值，如果未找到返回null
     */
    public static SupportedPeriod fromPeriod(String period) {
        for (SupportedPeriod p : values()) {
            if (p.period.equals(period)) {
                return p;
            }
        }
        return null;
    }
}

