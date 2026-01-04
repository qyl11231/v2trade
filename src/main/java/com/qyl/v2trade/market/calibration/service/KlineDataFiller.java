package com.qyl.v2trade.market.calibration.service;

import com.qyl.v2trade.market.model.NormalizedKline;

import java.util.List;

/**
 * 数据补全服务接口
 * 用于将拉取的数据插入到QuestDB
 */
public interface KlineDataFiller {

    /**
     * 填充缺失的K线数据
     * 
     * @param symbol 交易对symbol（交易所格式，如 BTC-USDT-SWAP）
     * @param klines K线数据列表
     * @return 成功插入的数量
     */
    int fillMissingKlines(String symbol, List<NormalizedKline> klines);
}

