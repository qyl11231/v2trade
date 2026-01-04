package com.qyl.v2trade.common.constants;

/**
 * 策略决策模式常量
 */
public class DecisionMode {
    
    /**
     * 完全跟随信号，信号即指令
     */
    public static final String FOLLOW_SIGNAL = "FOLLOW_SIGNAL";
    
    /**
     * 信号作为意图，由策略自行决定是否/何时兑现
     */
    public static final String INTENT_DRIVEN = "INTENT_DRIVEN";
}

