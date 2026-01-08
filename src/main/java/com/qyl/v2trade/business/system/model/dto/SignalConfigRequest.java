package com.qyl.v2trade.business.system.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 信号配置请求DTO
 */
@Data
public class SignalConfigRequest {

    /**
     * API Key ID
     */
    @NotNull(message = "API Key ID不能为空")
    private Long apiKeyId;

    /**
     * 信号名称（TradingView strategy name）
     */
    @NotBlank(message = "信号名称不能为空")
    private String signalName;

    /**
     * 交易对，如 BTC-USDT（用于TradingView webhook匹配）
     */
    @NotBlank(message = "交易对不能为空")
    private String symbol;

    /**
     * 关联交易对ID
     */
    private Long tradingPairId;

    /**
     * 是否启用：1-启用 0-禁用
     */
    private Integer enabled;
}
