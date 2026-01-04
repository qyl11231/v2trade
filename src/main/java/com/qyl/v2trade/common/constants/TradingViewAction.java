package com.qyl.v2trade.common.constants;

/**
 * TradingView Action 常量
 * 用于匹配 TradingView Webhook 中的 action 字段
 */
public class TradingViewAction {

    /**
     * 买入
     */
    public static final String BUY = "BUY";

    /**
     * 卖出
     */
    public static final String SELL = "SELL";

    /**
     * 做多
     */
    public static final String LONG = "LONG";

    /**
     * 做空
     */
    public static final String SHORT = "SHORT";

    /**
     * 平仓
     */
    public static final String CLOSE = "CLOSE";

    private TradingViewAction() {
        // 工具类，禁止实例化
    }
}
