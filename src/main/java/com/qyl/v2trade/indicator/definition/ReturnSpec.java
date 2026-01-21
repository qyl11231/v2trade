package com.qyl.v2trade.indicator.definition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 返回规范
 * 
 * <p>根据《指标参数配置.md》要求：
 * - 输出类型：SCALAR / SIGNAL / SERIES / STATE
 * - 生成的 Schema 格式：{ "value": { "type": "SCALAR" }, "signal": { "type": "SIGNAL" } }
 */
public record ReturnSpec(
    /**
     * 返回类型：单值或多值
     */
    ReturnType type,
    
    /**
     * 多值时的键列表（如BOLL的upper/middle/lower）
     */
    List<String> keys
) {
    /**
     * 返回类型
     */
    public enum ReturnType {
        SINGLE,  // 单值（如RSI, SMA）
        MULTI    // 多值（如BOLL, MACD）
    }
    
    /**
     * 创建单值返回规范
     */
    public static ReturnSpec single() {
        return new ReturnSpec(ReturnType.SINGLE, List.of());
    }
    
    /**
     * 创建多值返回规范
     */
    public static ReturnSpec multi(List<String> keys) {
        return new ReturnSpec(ReturnType.MULTI, keys);
    }
    
    /**
     * 转换为Map（用于存储JSON）
     * 
     * <p>生成格式符合前端期望：
     * <pre>
     * // 单值返回
     * { "value": { "type": "SCALAR" } }
     * 
     * // 多值返回（如BOLL）
     * {
     *   "upper": { "type": "SCALAR" },
     *   "middle": { "type": "SCALAR" },
     *   "lower": { "type": "SCALAR" }
     * }
     * </pre>
     * 
     * <p>注意：当前所有指标输出都是数值型（SCALAR），未来可扩展为 SIGNAL/SERIES/STATE
     */
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        
        if (type == ReturnType.SINGLE) {
            // 单值返回：生成 { "value": { "type": "SCALAR" } }
            Map<String, Object> valueSpec = new HashMap<>();
            valueSpec.put("type", "SCALAR");
            result.put("value", valueSpec);
        } else if (type == ReturnType.MULTI && keys != null && !keys.isEmpty()) {
            // 多值返回：为每个 key 生成一个输出定义
            for (String key : keys) {
                Map<String, Object> outputSpec = new HashMap<>();
                outputSpec.put("type", "SCALAR"); // 当前所有输出都是数值型
                result.put(key, outputSpec);
            }
        }
        
        return result;
    }
}
