package com.qyl.v2trade.market.calibration.service.dto;

import lombok.Data;

/**
 * 交易对信息DTO
 * 用于校准模块获取交易对的完整信息
 */
@Data
public class TradingPairInfo {

    /**
     * 交易对ID
     */
    private Long tradingPairId;

    /**
     * 标准化symbol（如 BTC-USDT）
     */
    private String symbol;

    /**
     * 交易所格式（如 BTC-USDT-SWAP）
     */
    private String symbolOnExchange;

    /**
     * 交易所代码（如 OKX）
     */
    private String exchangeCode;
}

