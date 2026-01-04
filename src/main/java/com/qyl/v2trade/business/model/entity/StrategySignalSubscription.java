package com.qyl.v2trade.business.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 策略信号订阅实体
 */
@Data
@TableName("strategy_signal_subscription")
public class StrategySignalSubscription implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 订阅ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 策略ID
     */
    private Long strategyId;

    /**
     * 信号配置ID
     */
    private Long signalConfigId;

    /**
     * 消费模式：LATEST_ONLY
     */
    private String consumeMode;

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

