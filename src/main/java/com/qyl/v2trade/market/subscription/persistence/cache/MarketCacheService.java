package com.qyl.v2trade.market.subscription.persistence.cache;

import com.qyl.v2trade.market.model.NormalizedKline;

import java.util.List;

/**
 * 行情缓存服务接口
 * 使用Redis缓存热点K线数据
 */
public interface MarketCacheService {

    /**
     * 缓存K线数据
     * 
     * @param kline K线数据
     * @param cacheDurationMinutes 缓存时长（分钟）
     */
    void cacheKline(NormalizedKline kline, int cacheDurationMinutes);

    /**
     * 从缓存获取K线数据
     * 
     * @param symbol 交易对符号
     * @param interval K线周期
     * @param timestamp 时间戳（毫秒）
     * @return K线数据，不存在返回null
     */
    NormalizedKline getKlineFromCache(String symbol, String interval, long timestamp);

    /**
     * 从缓存获取K线列表（时间范围）
     * 
     * @param symbol 交易对符号
     * @param interval K线周期
     * @param fromTimestamp 开始时间戳（毫秒）
     * @param toTimestamp 结束时间戳（毫秒）
     * @return K线列表
     */
    List<NormalizedKline> getKlinesFromCache(String symbol, String interval, 
                                             long fromTimestamp, long toTimestamp);

    /**
     * 清除缓存
     * 
     * @param symbol 交易对符号
     * @param interval K线周期
     */
    void clearCache(String symbol, String interval);
}

