package com.qyl.v2trade.market.calibration.service;

import java.time.Instant;
import java.util.List;

/**
 * K线缺口检测服务接口
 * 用于检测缺失的K线时间点
 * 
 * <p>时间语义：所有时间都是UTC Instant。
 */
public interface KlineGapDetector {

    /**
     * 检测缺失的K线时间点
     * 
     * <p>时间边界：左闭右开区间 [startTime, endTime)
     * 
     * @param tradingPairId 交易对ID
     * @param startTime 开始时间（Instant，UTC，包含）
     * @param endTime 结束时间（Instant，UTC，不包含）
     * @return 缺失的时间戳列表（毫秒，UTC epoch millis，分钟起始点）
     */
    List<Long> detectMissingTimestamps(Long tradingPairId, Instant startTime, Instant endTime);
}

