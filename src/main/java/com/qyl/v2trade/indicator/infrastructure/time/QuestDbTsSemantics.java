package com.qyl.v2trade.indicator.infrastructure.time;

/**
 * QuestDB ts字段语义
 * 
 * <p>由QuestDbTsSemanticsProbe在启动时自动检测并设置
 *
 * @author qyl
 */
public enum QuestDbTsSemantics {
    /**
     * ts表示开盘时间（openTime）
     * 转换规则：bar_close_time = ts + timeframe_duration
     */
    TS_IS_OPEN_TIME,
    
    /**
     * ts表示收盘时间（closeTime）
     * 转换规则：bar_close_time = ts
     */
    TS_IS_CLOSE_TIME,
    
    /**
     * 无法判定（系统启动失败）
     */
    UNKNOWN
}

