package com.qyl.v2trade.business.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 策略定义视图对象
 */
@Data
public class StrategyDefinitionVO {

    /**
     * 策略ID
     */
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 策略名称
     */
    private String strategyName;

    /**
     * 策略类型
     */
    private String strategyType;

    /**
     * 策略行为
     */
    private String decisionMode;

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

