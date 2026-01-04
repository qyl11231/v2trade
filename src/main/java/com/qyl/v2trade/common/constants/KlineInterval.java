package com.qyl.v2trade.common.constants;

/**
 * K线周期枚举
 */
public enum KlineInterval {
    
    /**
     * 1分钟
     */
    ONE_MINUTE("1m", 1),
    
    /**
     * 5分钟
     */
    FIVE_MINUTE("5m", 5),
    
    /**
     * 15分钟
     */
    FIFTEEN_MINUTE("15m", 15),
    
    /**
     * 30分钟
     */
    THIRTY_MINUTE("30m", 30),
    
    /**
     * 1小时
     */
    ONE_HOUR("1h", 60),
    
    /**
     * 4小时
     */
    FOUR_HOUR("4h", 240);

    /**
     * 周期标识（如：1m, 5m）
     */
    private final String interval;

    /**
     * 分钟数
     */
    private final int minutes;

    KlineInterval(String interval, int minutes) {
        this.interval = interval;
        this.minutes = minutes;
    }

    public String getInterval() {
        return interval;
    }

    public int getMinutes() {
        return minutes;
    }

    /**
     * 根据标识获取枚举
     */
    public static KlineInterval fromInterval(String interval) {
        for (KlineInterval klineInterval : values()) {
            if (klineInterval.interval.equals(interval)) {
                return klineInterval;
            }
        }
        throw new IllegalArgumentException("Unknown interval: " + interval);
    }

    /**
     * 判断是否为有效的周期标识
     */
    public static boolean isValid(String interval) {
        try {
            fromInterval(interval);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}

