package com.qyl.v2trade.business.system.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * API Key请求DTO
 */
@Data
public class ApiKeyRequest {

    /**
     * 交易所
     */
    @NotBlank(message = "交易所不能为空")
    private String exchange;

    /**
     * API Key
     */
    @NotBlank(message = "API Key不能为空")
    private String apiKey;

    /**
     * Secret Key
     */
    @NotBlank(message = "Secret Key不能为空")
    private String secretKey;

    /**
     * Passphrase
     */
    private String passphrase;

    /**
     * API Key名称
     */
    private String apiKeyName;

    /**
     * 备注
     */
    private String remark;
}
