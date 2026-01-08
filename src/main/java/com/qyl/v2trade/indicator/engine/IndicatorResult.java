package com.qyl.v2trade.indicator.engine;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 指标计算结果
 *
 * @author qyl
 */
public record IndicatorResult(
    /**
     * 计算状态
     */
    Status status,
    
    /**
     * 指标值
     * 
     * <p>单值指标：Map只包含一个键，如 {"value": 65.5}
     * <p>多值指标：Map包含多个键，如 {"macd": 12.3, "signal": 11.8, "histogram": 0.5}
     * <p>如果status为INVALID，此字段为null
     */
    Map<String, BigDecimal> values,
    
    /**
     * 错误信息（仅在status为INVALID时使用）
     */
    String errorMessage
) {
    /**
     * 计算状态
     */
    public enum Status {
        /**
         * 计算成功
         */
        SUCCESS,
        
        /**
         * 计算失败（参数错误、数据不足等）
         */
        INVALID
    }
    
    /**
     * 创建成功结果（单值）
     */
    public static IndicatorResult success(String key, BigDecimal value) {
        return new IndicatorResult(
                Status.SUCCESS,
                Map.of(key, value),
                null
        );
    }
    
    /**
     * 创建成功结果（多值）
     */
    public static IndicatorResult success(Map<String, BigDecimal> values) {
        return new IndicatorResult(
                Status.SUCCESS,
                values,
                null
        );
    }
    
    /**
     * 创建失败结果
     */
    public static IndicatorResult invalid(String errorMessage) {
        return new IndicatorResult(
                Status.INVALID,
                null,
                errorMessage
        );
    }
    
    /**
     * 获取单值（适用于单值指标）
     */
    public BigDecimal getSingleValue() {
        if (status != Status.SUCCESS || values == null || values.isEmpty()) {
            return null;
        }
        return values.values().iterator().next();
    }
    
    /**
     * 获取指定键的值（适用于多值指标）
     */
    public BigDecimal getValue(String key) {
        if (status != Status.SUCCESS || values == null) {
            return null;
        }
        return values.get(key);
    }
}

