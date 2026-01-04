package com.qyl.v2trade.business.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 信号视图对象
 */
@Data
public class SignalVO {

    /**
     * 信号ID
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
     * 信号配置ID
     */
    private Long signalConfigId;

    /**
     * 信号来源
     */
    private String signalSource;

    /**
     * 信号名称
     */
    private String signalName;

    /**
     * 交易对
     */
    private String symbol;

    /**
     * 信号事件类型
     */
    private String signalEventType;

    /**
     * 信号方向提示
     */
    private String signalDirectionHint;

    /**
     * 信号参考价格
     */
    private BigDecimal price;

    /**
     * 信号建议数量
     */
    private BigDecimal quantity;

    /**
     * 原始信号内容（JSON字符串）
     */
    private String rawPayload;

    /**
     * 信号接收时间
     */
    private LocalDateTime receivedAt;

    /**
     * 入库时间
     */
    private LocalDateTime createdAt;
}
