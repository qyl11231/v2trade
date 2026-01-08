package com.qyl.v2trade.business.system.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * API Key信息VO
 */
@Data
public class ApiKeyVO {

    /**
     * ID
     */
    private Long id;

    /**
     * 交易所
     */
    private String exchange;

    /**
     * API Key（脱敏显示）
     */
    private String apiKey;

    /**
     * Secret Key（脱敏显示）
     */
    private String secretKey;

    private String passphrase;

    /**
     * API Key名称
     */
    private String apiKeyName;

    /**
     * 状态：1-启用 0-禁用
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
