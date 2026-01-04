package com.qyl.v2trade.common.constants;

/**
 * 信号事件类型常量
 * 用于统一管理信号事件类型的值
 */
public class SignalEventType {

    /**
     * 突破
     */
    public static final String BREAKOUT = "BREAKOUT";

    /**
     * 交叉
     */
    public static final String CROSS = "CROSS";

    /**
     * 超卖
     */
    public static final String OVERSOLD = "OVERSOLD";

    /**
     * 自定义
     */
    public static final String CUSTOM = "CUSTOM";

    private SignalEventType() {
        // 工具类，禁止实例化
    }
}
