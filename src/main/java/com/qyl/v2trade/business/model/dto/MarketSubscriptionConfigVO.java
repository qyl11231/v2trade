package com.qyl.v2trade.business.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 行情订阅配置VO
 */
@Data
public class MarketSubscriptionConfigVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 交易对ID
     */
    private Long tradingPairId;

    /**
     * 交易对符号（如：BTC-USDT）
     */
    private String symbol;

    /**
     * 市场类型（如：SWAP）
     */
    private String marketType;

    /**
     * 是否启用：1-启用 0-禁用
     */
    private Integer enabled;

    /**
     * Redis缓存时长（分钟）
     */
    private Integer cacheDurationMinutes;

    /**
     * 备注说明
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

    /**
     * 当前价格
     */
    private Double currentPrice;

    /**
     * 今日涨跌幅（百分比）
     */
    private Double todayChange;

    /**
     * 今日最高价
     */
    private Double todayHigh;

    /**
     * 今日最低价
     */
    private Double todayLow;

    /**
     * 今日成交量
     */
    private Double todayVolume;
}

