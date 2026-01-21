package com.qyl.v2trade.indicator.definition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 参数规范
 * 
 * <p>根据《指标参数配置.md》要求：
 * - 参数类型使用小写：int / float / bool / string
 * - 生成的 Schema 格式：{ "paramName": { "type": "int", "required": true, "default": 20, "min": 1, "max": 500 } }
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
        INT,      // 映射为 "int"
        DECIMAL,  // 映射为 "float"
        ENUM,     // 映射为 "string"（带 enum 字段）
        STRING    // 映射为 "string"
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
     * 
     * <p>生成格式符合前端期望：
     * <pre>
     * {
     *   "period": { "type": "int", "required": true, "default": 20, "min": 1, "max": 500 },
     *   "source": { "type": "string", "enum": ["open", "high", "low", "close"] }
     * }
     * </pre>
     */
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        
        for (ParameterDefinition param : parameters) {
            Map<String, Object> paramSpec = new HashMap<>();
            
            // 类型映射：INT -> "int", DECIMAL -> "float", ENUM/STRING -> "string"
            String typeStr;
            if (param.type() == ParamType.INT) {
                typeStr = "int";
            } else if (param.type() == ParamType.DECIMAL) {
                typeStr = "float";
            } else {
                typeStr = "string";
            }
            paramSpec.put("type", typeStr);
            
            // required
            paramSpec.put("required", param.required());
            
            // default（注意：前端使用 "default" 而不是 "defaultValue"）
            if (param.defaultValue() != null) {
                paramSpec.put("default", param.defaultValue());
            }
            
            // min/max（直接在参数对象中，不在 range 嵌套）
            if (param.range() != null) {
                if (param.range().min() != null) {
                    paramSpec.put("min", param.range().min());
                }
                if (param.range().max() != null) {
                    paramSpec.put("max", param.range().max());
                }
            }
            
            // 如果类型是 ENUM，需要添加 enum 字段（这里暂时不处理，因为当前定义中没有 enum 值）
            // 未来如果需要，可以在 ParameterDefinition 中添加 enum 字段
            
            result.put(param.name(), paramSpec);
        }
        
        return result;
    }
}
