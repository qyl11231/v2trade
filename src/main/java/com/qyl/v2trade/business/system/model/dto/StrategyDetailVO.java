package com.qyl.v2trade.business.system.model.dto;

import lombok.Data;

import java.util.List;

/**
 * 策略完整详情视图对象
 */
@Data
public class StrategyDetailVO {

    /**
     * 策略定义
     */
    private StrategyDefinitionVO definition;

    /**
     * 策略参数
     */
    private StrategyParamVO param;

    /**
     * 交易对列表
     */
    private List<StrategySymbolVO> tradingPairs;

    /**
     * 信号订阅列表
     */
    private List<StrategySignalSubscriptionVO> signalSubscriptions;
}

