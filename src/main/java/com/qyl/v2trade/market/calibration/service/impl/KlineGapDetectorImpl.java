package com.qyl.v2trade.market.calibration.service.impl;

import com.qyl.v2trade.market.calibration.service.KlineGapDetector;
import com.qyl.v2trade.market.calibration.service.QuestDbKlineQueryService;
import com.qyl.v2trade.market.calibration.service.TradingPairInfoService;
import com.qyl.v2trade.market.calibration.service.dto.TradingPairInfo;
import com.qyl.v2trade.market.calibration.util.KlineTimeCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qyl.v2trade.common.util.TimeUtil;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * K线缺口检测服务实现类
 */
@Slf4j
@Service
public class KlineGapDetectorImpl implements KlineGapDetector {

    @Autowired
    private TradingPairInfoService tradingPairInfoService;

    @Autowired
    private QuestDbKlineQueryService questDbKlineQueryService;

    @Override
    public List<Long> detectMissingTimestamps(Long tradingPairId, Instant startTime, Instant endTime) {
        try {
            // 1. 获取交易对信息
            TradingPairInfo pairInfo = tradingPairInfoService.getTradingPairInfo(tradingPairId);
            String symbolOnExchange = pairInfo.getSymbolOnExchange();

            // 重构：使用 Instant 参数，遵循时间管理约定
            List<Long> expectedTimestamps = KlineTimeCalculator.calculateExpectedTimestamps(
                    startTime, endTime);
            log.debug("期望的K线时间点数量: {} (对齐后范围: {} ~ {})", 
                    expectedTimestamps.size(), 
                    TimeUtil.formatWithBothTimezones(startTime),
                    TimeUtil.formatWithBothTimezones(endTime));

            if (expectedTimestamps.isEmpty()) {
                return new ArrayList<>();
            }

            // 3. 查询已存在的时间点列表（UTC epoch millis，已对齐到分钟起始点）
            // 重构：使用 Instant 参数，遵循时间管理约定
            List<Long> existingTimestamps = questDbKlineQueryService.queryExistingTimestamps(
                    symbolOnExchange, startTime, endTime);
            log.debug("已存在的K线时间点数量: {}", existingTimestamps.size());

            // 4. 确保existingTimestamps都对齐到分钟起始点（双重保险）
            List<Long> alignedExistingTimestamps = existingTimestamps.stream()
                    .map(KlineTimeCalculator::alignToMinuteStart)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
            log.debug("对齐后的已存在K线时间点数量: {}", alignedExistingTimestamps.size());

            // 5. 对比找出缺失的时间点
            Set<Long> existingSet = new HashSet<>(alignedExistingTimestamps);
            List<Long> missingTimestamps = expectedTimestamps.stream()
                    .filter(timestamp -> !existingSet.contains(timestamp))
                    .collect(Collectors.toList());

            log.info("检测缺失的K线时间点完成: tradingPairId={}, symbol={}, 期望={}, 已存在={} (对齐后), 缺失={}, " +
                    "时间范围: {} ~ {}", 
                    tradingPairId, symbolOnExchange, expectedTimestamps.size(), 
                    alignedExistingTimestamps.size(), missingTimestamps.size(),
                    TimeUtil.formatWithBothTimezones(startTime),
                    TimeUtil.formatWithBothTimezones(endTime));

            return missingTimestamps;
        } catch (Exception e) {
            log.error("检测缺失的K线时间点失败: tradingPairId={}, startTime={}, endTime={}", 
                    tradingPairId, 
                    startTime != null ? TimeUtil.formatWithBothTimezones(startTime) : "null",
                    endTime != null ? TimeUtil.formatWithBothTimezones(endTime) : "null", e);
            throw new RuntimeException("检测缺失K线失败: " + e.getMessage(), e);
        }
    }
}

