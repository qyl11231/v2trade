package com.qyl.v2trade.indicator.repository.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 指标定义表实体（V2：单表设计，指标定义注册中心）
 * 
 * <p>【V2 核心变化】
 * - 只保留 indicator_definition 一张表
 * - data_source 和 impl_key 为独立字段（不再存储在 JSON 中）
 * - 通过 JSON 字段存储 param_limits 等扩展字段
 * - 支持用户自定义指标定义
 */
@Data
@TableName(value = "indicator_definition", autoResultMap = true)
public class IndicatorDefinition implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 所属用户ID（0=系统内置；>0=用户自定义）
     */
    private Long userId;
    
    /**
     * 指标编码（如"RSI"、"MACD"）
     */
    private String indicatorCode;
    
    /**
     * 指标名称（展示用）
     */
    private String indicatorName;
    
    /**
     * 指标版本（如"v1"）
     */
    private String indicatorVersion;
    
    /**
     * 指标分类（TREND/MOMENTUM/VOLATILITY/VOLUME/GENERAL）
     */
    private String category;
    
    /**
     * 计算引擎（ta4j/custom，向后兼容）
     */
    private String engine;
    
    /**
     * 数据源（BAR/TICK/SIGNAL/MIXED，V2新增独立字段）
     */
    private String dataSource;
    
    /**
     * 实现映射键（如 ta4j:rsi、builtin:price_breakout，V2新增独立字段）
     */
    private String implKey;
    
    /**
     * 参数Schema（JSON格式，用于前端表单渲染+后端校验）
     * 
     * <p>V2 扩展：可在 JSON 中存储 param_limits
     * <p>示例结构：
     * <pre>
     * {
     *   "period": {"type": "integer", "required": true, "default": 14, "min": 1, "max": 100},
     *   "param_limits": {"lookback_max": 365, "window_max": 1000}
     * }
     * </pre>
     * <p>注意：data_source 和 impl_key 已改为独立字段，不再存储在 JSON 中
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> paramSchema;
    
    /**
     * 返回Schema（JSON格式，用于输出校验+前端展示）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> returnSchema;
    
    /**
     * 最小所需bar数量
     */
    private Integer minRequiredBars;
    
    /**
     * 支持周期列表（JSON格式，如["5m", "15m", "30m", "1h", "4h"]）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> supportedTimeframes;
    
    /**
     * 是否启用（1=启用，0=禁用）
     */
    private Integer enabled;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    // ========== V2 扩展字段的便捷方法（从 paramSchema JSON 中提取） ==========
    
    /**
     * 获取参数限制（V2新增）
     * 
     * @return 参数限制 Map（如 {"lookback_max": 365, "window_max": 1000}），不存在返回 null
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getParamLimits() {
        if (paramSchema == null) {
            return null;
        }
        Object paramLimits = paramSchema.get("param_limits");
        if (paramLimits instanceof Map) {
            return (Map<String, Object>) paramLimits;
        }
        return null;
    }
    
    /**
     * 设置参数限制（V2新增）
     * 
     * @param paramLimits 参数限制 Map（如 {"lookback_max": 365, "window_max": 1000}）
     */
    public void setParamLimits(Map<String, Object> paramLimits) {
        if (paramSchema == null) {
            paramSchema = new HashMap<>();
        }
        if (paramLimits != null) {
            paramSchema.put("param_limits", paramLimits);
        } else {
            paramSchema.remove("param_limits");
        }
    }
}

