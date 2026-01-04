package com.qyl.v2trade.market.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 标准化K线数据模型
 * 统一所有交易所的K线格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NormalizedKline implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 交易对符号（标准化格式，如：BTC-USDT）
     */
    private String symbol;

    /**
     * K线周期（如：1m, 5m, 15m）
     */
    private String interval;

    /**
     * 开盘价
     */
    private Double open;

    /**
     * 最高价
     */
    private Double high;

    /**
     * 最低价
     */
    private Double low;

    /**
     * 收盘价
     */
    private Double close;

    /**
     * 成交量
     */
    private Double volume;

    /**
     * 时间戳（毫秒级）
     */
    private Long timestamp;

    /**
     * 交易所原始时间戳（用于去重）
     */
    private Long exchangeTimestamp;
}

