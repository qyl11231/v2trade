package com.qyl.v2trade.business.strategy.decision.logic.condition;

import lombok.Builder;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 条件规则
 * 
 * <p>表示一个单一的条件规则
 */
@Getter
@Builder
public class ConditionRule implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 因子key（如 "IND.RSI_14"）
     */
    private final String factor;

    /**
     * 操作符（如 "LT"）
     */
    private final String operator;

    /**
     * 比较值（可能是数值、字符串或因子引用）
     */
    private final Object value;

    /**
     * 值类型：NUMBER / STRING / BOOLEAN
     */
    private final String type;

    /**
     * 是否允许为空（false时缺值会block）
     */
    private final Boolean nullable;
}

