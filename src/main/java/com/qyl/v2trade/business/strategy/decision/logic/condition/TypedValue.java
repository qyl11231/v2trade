package com.qyl.v2trade.business.strategy.decision.logic.condition;

import lombok.Builder;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 类型化值
 * 
 * <p>用于条件评估中的类型安全值
 */
@Getter
@Builder
public class TypedValue implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 值类型
     */
    private final ValueType type;

    /**
     * 数值（当type=NUMBER时）
     */
    private final BigDecimal numberValue;

    /**
     * 字符串值（当type=STRING时）
     */
    private final String stringValue;

    /**
     * 布尔值（当type=BOOLEAN时）
     */
    private final Boolean booleanValue;

    /**
     * 值类型枚举
     */
    public enum ValueType {
        NUMBER, STRING, BOOLEAN
    }

    /**
     * 创建数值类型
     */
    public static TypedValue ofNumber(BigDecimal value) {
        return TypedValue.builder()
            .type(ValueType.NUMBER)
            .numberValue(value)
            .build();
    }

    /**
     * 创建字符串类型
     */
    public static TypedValue ofString(String value) {
        return TypedValue.builder()
            .type(ValueType.STRING)
            .stringValue(value)
            .build();
    }

    /**
     * 创建布尔类型
     */
    public static TypedValue ofBoolean(Boolean value) {
        return TypedValue.builder()
            .type(ValueType.BOOLEAN)
            .booleanValue(value)
            .build();
    }
}

