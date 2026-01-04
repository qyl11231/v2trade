package com.qyl.v2trade.business.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 交易对VO
 */
@Data
public class TradingPairVO {
    
    /**
     * ID
     */
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
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    // ===== 关联的交易规则信息（OKX） =====
    
    /**
     * 交易所内部交易对标识
     */
    private String symbolOnExchange;
    
    /**
     * 交易状态
     */
    private String tradingStatus;
    
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
    private String minOrderQty;
    
    /**
     * 最大杠杆
     */
    private Integer maxLeverage;
}

