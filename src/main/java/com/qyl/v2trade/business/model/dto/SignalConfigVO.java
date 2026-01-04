package com.qyl.v2trade.business.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 信号配置视图对象
 */
@Data
public class SignalConfigVO {

    /**
     * 信号配置ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * API Key ID
     */
    private Long apiKeyId;

    /**
     * 信号名称
     */
    private String signalName;

    /**
     * 交易对
     */
    private String symbol;

    /**
     * 关联交易对ID
     */
    private Long tradingPairId;

    /**
     * 市场类型（来自关联交易对）
     */
    private String marketType;

    /**
     * 是否启用：1-启用 0-禁用
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
