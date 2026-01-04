package com.qyl.v2trade.common.constants;

/**
 * 行情数据状态枚举
 */
public enum MarketDataStatus {
    
    /**
     * 初始化中（历史数据加载）
     */
    INIT("INIT", "初始化中"),
    
    /**
     * 实时运行中
     */
    LIVE("LIVE", "实时运行中"),
    
    /**
     * 补拉中
     */
    BACKFILLING("BACKFILLING", "补拉中"),
    
    /**
     * 检测到缺口
     */
    GAP_DETECTED("GAP_DETECTED", "检测到缺口"),
    
    /**
     * 已停止
     */
    STOPPED("STOPPED", "已停止");

    private final String code;
    private final String description;

    MarketDataStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据代码获取枚举
     */
    public static MarketDataStatus fromCode(String code) {
        for (MarketDataStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status code: " + code);
    }
}

