package com.qyl.v2trade.indicator.domain.event;

/**
 * Bar事件类型
 * 
 * <p>用于区分K线的状态
 *
 * @author qyl
 */
public enum BarEventType {
    /**
     * 形成中的K线（不允许指标计算）
     */
    FORMING_UPDATE,
    
    /**
     * 已闭合的K线（唯一合法计算点）
     */
    BAR_CLOSED
}

