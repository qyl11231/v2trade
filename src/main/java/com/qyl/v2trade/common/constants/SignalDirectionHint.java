package com.qyl.v2trade.common.constants;

/**
 * 信号方向提示常量
 * 用于统一管理信号方向提示的值
 */
public class SignalDirectionHint {

    /**
     * 做多/买入方向
     */
    public static final String LONG = "LONG";

    /**
     * 做空/卖出方向
     */
    public static final String SHORT = "SHORT";

    /**
     * 中性/平仓
     */
    public static final String NEUTRAL = "NEUTRAL";

    private SignalDirectionHint() {
        // 工具类，禁止实例化
    }
}
