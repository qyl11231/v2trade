package com.qyl.v2trade.market.calibration.service;

import com.qyl.v2trade.market.model.NormalizedKline;

import java.util.List;

/**
 * 历史K线数据拉取服务接口
 */
public interface HistoricalKlineFetcher {

    /**
     * 从OKX API拉取历史K线数据
     *
     * @param symbolOnExchange 交易所格式symbol（如 BTC-USDT-SWAP）
     * @param timestamps       需要拉取的时间戳列表（毫秒）
     * @return 拉取的K线数据列表
     */
    List<NormalizedKline> fetchHistoricalKlines(String symbolOnExchange, List<Long> timestamps);
}

