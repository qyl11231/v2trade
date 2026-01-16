package com.qyl.v2trade.business.strategy.model.dto;

import com.qyl.v2trade.business.system.model.dto.TradingPairVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 策略实例详情视图对象
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class StrategyInstanceDetailVO extends StrategyInstanceVO {

    /**
     * 策略定义详情
     */
    private StrategyDefinitionVO strategyDefinition;

    /**
     * 交易对详情（可选）
     */
    private TradingPairVO tradingPair;

    /**
     * 信号配置详情（可选，需要从signal模块获取）
     */
    private Object signalConfig;
}

