package com.qyl.v2trade.market.subscription.persistence.storage;

import com.qyl.v2trade.market.model.NormalizedKline;

import java.util.List;

/**
 * 行情存储服务接口
 */
public interface MarketStorageService {

    /**
     * 保存单根K线到QuestDB
     * 
     * @param kline 标准化K线
     * @return 是否保存成功
     */
    boolean saveKline(NormalizedKline kline);

    /**
     * 批量保存K线到QuestDB
     * 
     * @param klines K线列表
     * @return 成功保存的数量
     */
    int batchSaveKlines(List<NormalizedKline> klines);

    /**
     * 检查K线是否已存在（用于去重）
     * 
     * @param symbol 交易对符号
     * @param timestamp 时间戳（毫秒）
     * @return 是否存在
     */
    boolean exists(String symbol, long timestamp);
}

