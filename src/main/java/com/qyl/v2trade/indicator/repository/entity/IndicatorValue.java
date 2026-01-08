package com.qyl.v2trade.indicator.repository.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 指标值表实体
 */
@Data
@TableName(value = "indicator_value", autoResultMap = true)
public class IndicatorValue implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private Long tradingPairId;
    
    private String symbol;
    
    private String marketType;
    
    private String timeframe;
    
    private LocalDateTime barTime;
    
    private String indicatorCode;
    
    private String indicatorVersion;
    
    private BigDecimal value;
    
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, BigDecimal> extraValues;
    
    private String dataQuality;
    
    private String calcEngine;
    
    private String calcFingerprint;
    
    private Integer calcCostMs;
    
    private String source;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

