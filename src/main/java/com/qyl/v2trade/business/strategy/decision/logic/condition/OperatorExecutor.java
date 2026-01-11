package com.qyl.v2trade.business.strategy.decision.logic.condition;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 操作符执行器
 * 
 * <p>职责：
 * <ul>
 *   <li>执行类型安全的比较操作</li>
 *   <li>支持类型转换（NUMBER <-> STRING）</li>
 *   <li>处理异常情况</li>
 * </ul>
 */
@Slf4j
@Component
public class OperatorExecutor {

    /**
     * 执行比较
     * 
     * @param left 左值
     * @param operator 操作符
     * @param right 右值
     * @return 比较结果
     * @throws IllegalArgumentException 如果操作符不支持或类型不匹配
     */
    public boolean execute(TypedValue left, Operator operator, TypedValue right) {
        if (left == null || right == null) {
            throw new IllegalArgumentException("左值或右值不能为null");
        }

        // 类型检查
        if (left.getType() != right.getType()) {
            // 尝试类型转换（NUMBER <-> STRING）
            if (canConvert(left, right)) {
                return executeWithConversion(left, operator, right);
            }
            throw new IllegalArgumentException(
                String.format("类型不匹配: left=%s, right=%s", left.getType(), right.getType()));
        }

        // 根据类型执行比较
        switch (left.getType()) {
            case NUMBER:
                return executeNumberComparison(left.getNumberValue(), operator, right.getNumberValue());
            case STRING:
                return executeStringComparison(left.getStringValue(), operator, right.getStringValue());
            case BOOLEAN:
                return executeBooleanComparison(left.getBooleanValue(), operator, right.getBooleanValue());
            default:
                throw new IllegalArgumentException("不支持的类型: " + left.getType());
        }
    }

    /**
     * 数值比较
     */
    private boolean executeNumberComparison(BigDecimal left, Operator operator, BigDecimal right) {
        int comparison = left.compareTo(right);
        switch (operator) {
            case GT:
                return comparison > 0;
            case LT:
                return comparison < 0;
            case GTE:
                return comparison >= 0;
            case LTE:
                return comparison <= 0;
            case EQ:
                return comparison == 0;
            case NEQ:
                return comparison != 0;
            case BETWEEN:
            case NOT_BETWEEN:
                throw new UnsupportedOperationException("BETWEEN操作符需要特殊处理（当前版本不支持）");
            default:
                throw new IllegalArgumentException("数值类型不支持的操作符: " + operator);
        }
    }

    /**
     * 字符串比较
     */
    private boolean executeStringComparison(String left, Operator operator, String right) {
        switch (operator) {
            case EQ:
                return left.equals(right);
            case NEQ:
                return !left.equals(right);
            case CONTAINS:
                return left.contains(right);
            case STARTS_WITH:
                return left.startsWith(right);
            case ENDS_WITH:
                return left.endsWith(right);
            default:
                throw new IllegalArgumentException("字符串类型不支持的操作符: " + operator);
        }
    }

    /**
     * 布尔比较
     */
    private boolean executeBooleanComparison(Boolean left, Operator operator, Boolean right) {
        switch (operator) {
            case EQ:
                return left.equals(right);
            case NEQ:
                return !left.equals(right);
            default:
                throw new IllegalArgumentException("布尔类型不支持的操作符: " + operator);
        }
    }

    /**
     * 判断是否可以类型转换
     */
    private boolean canConvert(TypedValue left, TypedValue right) {
        return (left.getType() == TypedValue.ValueType.NUMBER && 
                right.getType() == TypedValue.ValueType.STRING) ||
               (left.getType() == TypedValue.ValueType.STRING && 
                right.getType() == TypedValue.ValueType.NUMBER);
    }

    /**
     * 执行类型转换后的比较
     */
    private boolean executeWithConversion(TypedValue left, Operator operator, TypedValue right) {
        try {
            if (left.getType() == TypedValue.ValueType.STRING) {
                BigDecimal leftNum = new BigDecimal(left.getStringValue());
                return executeNumberComparison(leftNum, operator, right.getNumberValue());
            } else {
                BigDecimal rightNum = new BigDecimal(right.getStringValue());
                return executeNumberComparison(left.getNumberValue(), operator, rightNum);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无法转换类型: " + e.getMessage());
        }
    }
}

