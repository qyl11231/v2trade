package com.qyl.v2trade.business.system.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 交易对主表（平台级，区分现货/合约）
 */
@Data
@TableName("trading_pair")
public class TradingPair implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 标准化交易对，如 BTC-USDT
     */
    private String symbol;

    /**
     * 基础货币，如 BTC
     */
    private String baseCurrency;

    /**
     * 计价货币，如 USDT
     */
    private String quoteCurrency;

    /**
     * 市场类型：SPOT / SWAP / FUTURES
     */
    private String marketType;

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

