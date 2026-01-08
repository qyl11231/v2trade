package com.qyl.v2trade.business.system.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 策略参数请求DTO
 */
@Data
public class StrategyParamRequest {

    /**
     * 策略ID
     */
    @NotNull(message = "策略ID不能为空")
    private Long strategyId;

    /**
     * 策略初始虚拟资金
     */
    @NotNull(message = "初始资金不能为空")
    private BigDecimal initialCapital;

    /**
     * 单次下单资金占比
     */
    @NotNull(message = "单次下单比例不能为空")
    private BigDecimal baseOrderRatio;

    /**
     * 策略止盈比例
     */
    private BigDecimal takeProfitRatio;

    /**
     * 策略止损比例
     */
    private BigDecimal stopLossRatio;

    /**
     * 策略入场条件（JSON字符串）
     */
    private String entryCondition;

    /**
     * 策略退出条件（JSON字符串）
     */
    private String exitCondition;
}

