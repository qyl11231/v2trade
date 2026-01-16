package com.qyl.v2trade.business.strategy.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 策略定义创建请求DTO
 */
@Data
public class StrategyDefinitionCreateRequest {

    /**
     * 策略名称（64字符）
     */
    @NotBlank(message = "策略名称不能为空")
    @Size(max = 64, message = "策略名称长度不能超过64个字符")
    private String strategyName;

    /**
     * 策略类型：Martin（马丁策略）、Grid（网格策略）、Trend（趋势策略）、Arbitrage（套利策略）
     */
    @NotBlank(message = "策略类型不能为空")
    private String strategyType;

    /**
     * 策略模式：SIGNAL_DRIVEN（信号驱动）、INDICATOR_DRIVEN（指标/因子驱动）、HYBRID（混合策略）
     */
    @NotBlank(message = "策略模式不能为空")
    private String strategyPattern;

    /**
     * 是否启用：1-启用 0-禁用（可选，默认1）
     */
    private Integer enabled;
}

