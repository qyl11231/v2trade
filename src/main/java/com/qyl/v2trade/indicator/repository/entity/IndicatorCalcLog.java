package com.qyl.v2trade.indicator.repository.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 指标计算日志表实体
 */
@Data
@TableName("indicator_calc_log")
public class IndicatorCalcLog implements Serializable {
    
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
    
    private String calcEngine;
    
    private String status;
    
    private Integer costMs;
    
    private String errorMsg;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

