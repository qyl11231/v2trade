package com.qyl.v2trade.common.constants;

/**
 * 信号来源常量
 * 用于统一管理信号来源的值
 */
public class SignalSource {

    /**
     * TradingView
     */
    public static final String TRADING_VIEW = "tv";

    /**
     * 内部计算
     */
    public static final String INTERNAL = "internal";

    /**
     * 手工测试
     */
    public static final String MANUAL = "manual";

    private SignalSource() {
        // 工具类，禁止实例化
    }
}
