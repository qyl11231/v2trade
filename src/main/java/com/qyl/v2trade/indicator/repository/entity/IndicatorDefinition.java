package com.qyl.v2trade.indicator.repository.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 指标定义表实体
 */
@Data
@TableName(value = "indicator_definition", autoResultMap = true)
public class IndicatorDefinition implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private String indicatorCode;
    
    private String indicatorName;
    
    private String indicatorVersion;
    
    private String category;
    
    private String engine;
    
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> paramSchema;
    
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> returnSchema;
    
    private Integer minRequiredBars;
    
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> supportedTimeframes;
    
    private Integer enabled;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

