package com.qyl.v2trade.business.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 信号配置实体（白名单 & 路由）
 */
@Data
@TableName("signal_config")
public class SignalConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 信号配置ID
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
     * 信号名称（TradingView strategy name）
     */
    private String signalName;

    /**
     * 交易对，如 BTC-USDT（用于TradingView webhook匹配）
     */
    private String symbol;

    /**
     * 关联交易对ID（系统内部引用）
     */
    private Long tradingPairId;

    /**
     * 是否启用：1-启用 0-禁用
     */
    private Integer enabled;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
