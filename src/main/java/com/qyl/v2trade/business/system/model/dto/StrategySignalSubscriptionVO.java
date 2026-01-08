package com.qyl.v2trade.business.system.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 策略信号订阅视图对象
 */
@Data
public class StrategySignalSubscriptionVO {

    /**
     * 订阅ID
     */
    private Long id;

    /**
     * 策略ID
     */
    private Long strategyId;

    /**
     * 信号配置ID
     */
    private Long signalConfigId;

    /**
     * 信号配置名称（来自关联表）
     */
    private String signalConfigName;

    /**
     * 消费模式
     */
    private String consumeMode;

    /**
     * 是否启用
     */
    private Integer enabled;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

