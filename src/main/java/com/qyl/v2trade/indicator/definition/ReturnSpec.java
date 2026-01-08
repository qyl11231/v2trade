package com.qyl.v2trade.indicator.definition;

import java.util.List;
import java.util.Map;

/**
 * 返回规范
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
     */
    public Map<String, Object> toMap() {
        return Map.of(
            "type", type.name(),
            "keys", keys != null ? keys : List.of()
        );
    }
}
