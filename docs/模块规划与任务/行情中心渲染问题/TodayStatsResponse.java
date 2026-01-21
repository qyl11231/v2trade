package com.qyl.v2trade.market.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 今日统计响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodayStatsResponse {
    /**
     * 交易对符号
     */
    private String symbol;

    /**
     * 今日最高价
     */
    private Double todayHigh;

    /**
     * 今日最低价
     */
    private Double todayLow;

    /**
     * 今日涨跌幅（百分比）
     */
    private Double todayChange;

    /**
     * 今日成交量
     */
    private Double todayVolume;

    /**
     * 当前价格
     */
    private Double currentPrice;
}

