package com.qyl.v2trade.business.strategy.model.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 策略实例更新请求DTO
 */
@Data
public class StrategyInstanceUpdateRequest {

    /**
     * 绑定信号定义ID（可选，不提供或为null时保持原值）
     */
    private Long signalConfigId;

    /**
     * 策略初始资金
     */
    @NotNull(message = "初始资金不能为空")
    @DecimalMin(value = "0.00000001", message = "初始资金必须大于0")
    private BigDecimal initialCapital;

    /**
     * 策略止盈比例（可选，0-1之间）
     */
    @DecimalMin(value = "0", message = "止盈比例不能小于0")
    @DecimalMax(value = "1", message = "止盈比例不能大于1")
    private BigDecimal takeProfitRatio;

    /**
     * 策略止损比例（可选，0-1之间）
     */
    @DecimalMin(value = "0", message = "止损比例不能小于0")
    @DecimalMax(value = "1", message = "止损比例不能大于1")
    private BigDecimal stopLossRatio;
}

