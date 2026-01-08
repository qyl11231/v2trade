package com.qyl.v2trade.indicator.definition;

import java.util.List;
import java.util.Map;

/**
 * 参数规范
 */
public record ParameterSpec(
    /**
     * 参数列表
     */
    List<ParameterDefinition> parameters
) {
    /**
     * 参数定义
     */
    public record ParameterDefinition(
        String name,
        ParamType type,
        boolean required,
        Object defaultValue,
        Range range
    ) {}
    
    /**
     * 参数类型
     */
    public enum ParamType {
        INT,
        DECIMAL,
        ENUM,
        STRING
    }
    
    /**
     * 参数范围
     */
    public record Range(
        Object min,
        Object max
    ) {}
    
    /**
     * 创建ParameterSpec
     */
    public static ParameterSpec of(List<ParameterDefinition> parameters) {
        return new ParameterSpec(parameters);
    }
    
    /**
     * 转换为Map（用于存储JSON）
     */
    public Map<String, Object> toMap() {
        return Map.of(
            "parameters", parameters.stream()
                .map(p -> Map.of(
                    "name", p.name(),
                    "type", p.type().name(),
                    "required", p.required(),
                    "defaultValue", p.defaultValue() != null ? p.defaultValue() : "",
                    "range", p.range() != null ? Map.of(
                        "min", p.range().min(),
                        "max", p.range().max()
                    ) : Map.of()
                ))
                .toList()
        );
    }
}
