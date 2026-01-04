package com.qyl.v2trade.market.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * K线查询响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KlineResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 交易对符号
     */
    private String symbol;

    /**
     * K线周期
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
     * 时间字符串（格式化后的时间，用于前端直接显示）
     * 格式：yyyy-MM-dd HH:mm:ss（本地时间，默认UTC+8）
     */
    private String timeString;
}

