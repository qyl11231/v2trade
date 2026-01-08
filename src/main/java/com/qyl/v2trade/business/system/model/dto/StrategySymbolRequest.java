package com.qyl.v2trade.business.system.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 策略交易对请求DTO
 */
@Data
public class StrategySymbolRequest {

    /**
     * 策略ID
     */
    @NotNull(message = "策略ID不能为空")
    private Long strategyId;

    /**
     * 交易对ID
     */
    @NotNull(message = "交易对ID不能为空")
    private Long tradingPairId;

    /**
     * 是否启用：1-启用 0-禁用
     */
    private Integer enabled;
}

