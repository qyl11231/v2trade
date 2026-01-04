package com.qyl.v2trade.business.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 交易所交易规则表（规则唯一真相）
 */
@Data
@TableName(value = "exchange_market_pair", autoResultMap = true)
public class ExchangeMarketPair implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 交易所代码：OKX / BINANCE
     */
    private String exchangeCode;

    /**
     * 关联交易对ID
     */
    private Long tradingPairId;

    /**
     * 交易所内部交易对标识，如 BTC-USDT / BTC-USDT-SWAP
     */
    private String symbolOnExchange;

    /**
     * 状态：TRADING / SUSPENDED
     */
    private String status;

    /**
     * 价格精度
     */
    private Integer pricePrecision;

    /**
     * 数量精度
     */
    private Integer quantityPrecision;

    /**
     * 最小下单数量
     */
    private BigDecimal minOrderQty;

    /**
     * 最小下单金额
     */
    private BigDecimal minOrderAmount;

    /**
     * 最大下单数量
     */
    private BigDecimal maxOrderQty;

    /**
     * 最大杠杆倍数
     */
    private Integer maxLeverage;

    /**
     * 原始响应数据
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> rawPayload;

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

