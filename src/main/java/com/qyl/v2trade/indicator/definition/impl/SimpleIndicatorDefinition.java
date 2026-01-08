package com.qyl.v2trade.indicator.definition.impl;

import com.qyl.v2trade.indicator.definition.IndicatorCategory;
import com.qyl.v2trade.indicator.definition.IndicatorDefinition;
import com.qyl.v2trade.indicator.definition.ParameterSpec;
import com.qyl.v2trade.indicator.definition.ReturnSpec;

import java.util.Set;

/**
 * 简单的指标定义实现
 */
public record SimpleIndicatorDefinition(
    String code,
    String version,
    String name,
    IndicatorCategory category,
    String engine,
    ParameterSpec parameters,
    ReturnSpec returns,
    int minRequiredBars,
    Set<String> supportedTimeframes
) implements IndicatorDefinition {
}

