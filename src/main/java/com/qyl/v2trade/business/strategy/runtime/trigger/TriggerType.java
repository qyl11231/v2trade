package com.qyl.v2trade.business.strategy.runtime.trigger;

/**
 * 触发类型枚举
 * 
 * <p>定义三类事件类型：
 * <ul>
 *   <li>BAR_CLOSE: K线闭合事件</li>
 *   <li>PRICE: 价格更新事件</li>
 *   <li>SIGNAL: 外部信号事件</li>
 * </ul>
 *
 * @author qyl
 */
public enum TriggerType {
    /**
     * K线闭合事件
     */
    BAR_CLOSE,
    
    /**
     * 价格更新事件
     */
    PRICE,
    
    /**
     * 外部信号事件
     */
    SIGNAL
}

