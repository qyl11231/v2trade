package com.qyl.v2trade.market.calibration.service;

import java.time.Instant;
import java.util.List;

/**
 * QuestDB K线查询服务接口
 * 用于校准模块查询已存在的K线数据
 * 
 * <p>时间语义：所有时间都是UTC Instant。
 */
public interface QuestDbKlineQueryService {

    /**
     * 查询指定时间范围内已存在的K线时间戳列表（1分钟K线）
     * 
     * <p>时间边界：左闭右开区间 [startTime, endTime)
     * 
     * @param symbol 交易对symbol（交易所格式，如 BTC-USDT-SWAP）
     * @param startTime 开始时间（Instant，UTC，包含）
     * @param endTime 结束时间（Instant，UTC，不包含）
     * @return 已存在的时间戳列表（毫秒，UTC epoch millis）
     */
    List<Long> queryExistingTimestamps(String symbol, Instant startTime, Instant endTime);
}

