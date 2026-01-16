package com.qyl.v2trade.business.strategy.model.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 策略定义详情视图对象（含实例列表）
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class StrategyDefinitionDetailVO extends StrategyDefinitionVO {

    /**
     * 实例列表
     */
    private List<StrategyInstanceVO> instances;
}

