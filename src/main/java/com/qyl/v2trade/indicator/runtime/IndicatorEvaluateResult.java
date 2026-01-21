package com.qyl.v2trade.indicator.runtime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 指标评估结果（V2新增）
 * 
 * <p>【🔴 输出边界：只返回计算结果，不返回策略语义】
 * 
 * <p>【允许的输出】：
 * - ✅ values / extraValues：指标计算值（数值结果）
 * - ✅ valid：是否有效（基于 min_required_bars）
 * - ✅ fingerprint：计算指纹（用于策略归档）
 * - ✅ source：数据来源（CACHE/COMPUTED）
 * - ✅ costMs：计算耗时
 * 
 * <p>【禁止的输出】：
 * - ❌ tradeAction、positionSide、signalScore、entryFlag、exitFlag、weight 等策略语义
 *
 * @author qyl
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndicatorEvaluateResult {
    
    /**
     * 是否有效（基于 min_required_bars）
     * 
     * <p>true：计算成功，结果可用
     * <p>false：计算失败或数据不足，结果不可用
     */
    private boolean valid;
    
    /**
     * 数据来源
     * 
     * <p>CACHE：来自缓存
     * <p>COMPUTED：实时计算
     */
    private String source;
    
    /**
     * 指标计算值（单值或多值）
     * 
     * <p>单值指标：Map只包含一个键，如 {"value": 65.5}
     * <p>多值指标：Map包含多个键，如 {"macd": 12.3, "signal": 11.8, "histogram": 0.5}
     */
    private Map<String, BigDecimal> values;
    
    /**
     * 计算指纹（用于策略归档）
     * 
     * <p>格式：SHA-256(code:version:params:engine)
     * <p>用于唯一标识一次计算配置，便于策略归档和可解释性
     */
    private String fingerprint;
    
    /**
     * 计算耗时（毫秒）
     */
    private Integer costMs;
    
    /**
     * 错误信息（如果有）
     * 
     * <p>当 valid=false 时，此字段包含错误原因
     */
    private String errorMsg;
    
    // ========== 便捷方法 ==========
    
    /**
     * 创建无效结果
     */
    public static IndicatorEvaluateResult invalid(String errorMsg) {
        return IndicatorEvaluateResult.builder()
                .valid(false)
                .source("COMPUTED")
                .values(null)
                .fingerprint(null)
                .costMs(0)
                .errorMsg(errorMsg)
                .build();
    }
    
    /**
     * 创建成功结果（单值）
     */
    public static IndicatorEvaluateResult success(String key, BigDecimal value, 
                                                  String fingerprint, Integer costMs, String source) {
        Map<String, BigDecimal> values = new HashMap<>();
        values.put(key, value);
        return IndicatorEvaluateResult.builder()
                .valid(true)
                .source(source)
                .values(values)
                .fingerprint(fingerprint)
                .costMs(costMs)
                .errorMsg(null)
                .build();
    }
    
    /**
     * 创建成功结果（多值）
     */
    public static IndicatorEvaluateResult success(Map<String, BigDecimal> values,
                                                  String fingerprint, Integer costMs, String source) {
        return IndicatorEvaluateResult.builder()
                .valid(true)
                .source(source)
                .values(values != null ? new HashMap<>(values) : null)
                .fingerprint(fingerprint)
                .costMs(costMs)
                .errorMsg(null)
                .build();
    }
    
    /**
     * 获取单值（适用于单值指标）
     */
    public BigDecimal getSingleValue() {
        if (!valid || values == null || values.isEmpty()) {
            return null;
        }
        return values.values().iterator().next();
    }
    
    /**
     * 获取指定键的值（适用于多值指标）
     */
    public BigDecimal getValue(String key) {
        if (!valid || values == null) {
            return null;
        }
        return values.get(key);
    }
}

