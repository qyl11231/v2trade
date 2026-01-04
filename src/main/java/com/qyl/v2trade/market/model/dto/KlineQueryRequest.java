package com.qyl.v2trade.market.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * K线查询请求DTO
 */
@Data
public class KlineQueryRequest {

    /**
     * 交易对符号（如：BTC-USDT）
     */
    @NotBlank(message = "交易对符号不能为空")
    private String symbol;

    /**
     * K线周期（如：1m, 5m, 15m）
     */
    @NotBlank(message = "K线周期不能为空")
    private String interval;

    /**
     * 开始时间（时间戳，毫秒级）
     */
    private Long from;

    /**
     * 结束时间（时间戳，毫秒级）
     */
    private Long to;

    /**
     * 限制返回数量（默认1000，最大10000）
     */
    private Integer limit;
}

