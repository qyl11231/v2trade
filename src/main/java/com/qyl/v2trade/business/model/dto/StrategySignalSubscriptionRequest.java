package com.qyl.v2trade.business.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 策略信号订阅请求DTO
 */
@Data
public class StrategySignalSubscriptionRequest {

    /**
     * 策略ID
     */
    @NotNull(message = "策略ID不能为空")
    private Long strategyId;

    /**
     * 信号配置ID
     */
    @NotNull(message = "信号配置ID不能为空")
    private Long signalConfigId;

    /**
     * 消费模式：LATEST_ONLY
     */
    private String consumeMode;

    /**
     * 是否启用：1-启用 0-禁用
     */
    private Integer enabled;
}

