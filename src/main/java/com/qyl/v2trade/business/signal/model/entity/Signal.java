package com.qyl.v2trade.business.signal.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 信号事实表实体（语义层，不含买卖）
 */
@Data
@TableName("`signal`")
public class Signal implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 信号ID
     */
    @TableId(type = IdType.AUTO)
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
     * 信号来源：tv / internal / manual
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
     * 信号事件类型：BREAKOUT / CROSS / OVERSOLD / CUSTOM（非交易动作）
     */
    private String signalEventType;

    /**
     * 信号方向提示：LONG / SHORT / NEUTRAL
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
     * 原始信号内容（完整保留，JSON格式）
     */
    private String rawPayload;

    /**
     * 信号接收时间
     */
    private LocalDateTime receivedAt;

    /**
     * 入库时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

