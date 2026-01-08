package com.qyl.v2trade.indicator.repository.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 指标订阅表实体
 */
@Data
@TableName(value = "indicator_subscription", autoResultMap = true)
public class IndicatorSubscription implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private Long tradingPairId;
    
    private String symbol;
    
    private String marketType;
    
    private String timeframe;
    
    private String indicatorCode;
    
    private String indicatorVersion;
    
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> params;
    
    private Integer enabled;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

