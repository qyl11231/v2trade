package com.qyl.v2trade.business.model.entity;

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
     * 策略ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 策略名称（用户维度唯一）
     */
    private String strategyName;

    /**
     * 策略类型：SIGNAL_DRIVEN / INDICATOR_DRIVEN / HYBRID
     */
    private String strategyType;

    /**
     * 策略行为：FOLLOW_SIGNAL / INTENT_DRIVEN
     */
    private String decisionMode;

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

