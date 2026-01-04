package com.qyl.v2trade.business.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 策略定义请求DTO
 */
@Data
public class StrategyDefinitionRequest {

    /**
     * 策略名称（用户维度唯一）
     */
    @NotBlank(message = "策略名称不能为空")
    private String strategyName;

    /**
     * 策略类型：SIGNAL_DRIVEN / INDICATOR_DRIVEN / HYBRID
     */
    @NotBlank(message = "策略类型不能为空")
    private String strategyType;

    /**
     * 策略行为：FOLLOW_SIGNAL / INTENT_DRIVEN
     */
    private String decisionMode;

    /**
     * 是否启用：1-启用 0-禁用
     */
    private Integer enabled;
}

