package com.qyl.v2trade.business.strategy.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 策略定义实体
 */
@Data
@TableName("strategy_definition")
public class StrategyDefinition implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 策略ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 策略名称（64字符）
     */
    private String strategyName;

    /**
     * 策略类型：Martin（马丁策略）、Grid（网格策略）、Trend（趋势策略）、Arbitrage（套利策略）
     */
    private String strategyType;

    /**
     * 策略模式：SIGNAL_DRIVEN（信号驱动）、INDICATOR_DRIVEN（指标/因子驱动）、HYBRID（混合策略）
     */
    private String strategyPattern;

    /**
     * 是否启用：1-启用 0-禁用
     */
    private Integer enabled;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

