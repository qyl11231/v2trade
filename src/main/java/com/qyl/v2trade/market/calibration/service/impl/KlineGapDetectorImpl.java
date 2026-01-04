package com.qyl.v2trade.market.calibration.service.impl;

import com.qyl.v2trade.market.calibration.service.KlineGapDetector;
import com.qyl.v2trade.market.calibration.service.QuestDbKlineQueryService;
import com.qyl.v2trade.market.calibration.service.TradingPairInfoService;
import com.qyl.v2trade.market.calibration.service.dto.TradingPairInfo;
import com.qyl.v2trade.market.calibration.util.KlineTimeCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
    public List<Long> detectMissingTimestamps(Long tradingPairId, long startTimestamp, long endTimestamp) {

        try {
            // 1. 获取交易对信息
            TradingPairInfo pairInfo = tradingPairInfoService.getTradingPairInfo(tradingPairId);
            String symbolOnExchange = pairInfo.getSymbolOnExchange();

            List<Long> expectedTimestamps = KlineTimeCalculator.calculateExpectedTimestamps(
                    startTimestamp, endTimestamp);
            log.debug("期望的K线时间点数量: {} (对齐后范围: {} ~ {})", 
                    expectedTimestamps.size(), startTimestamp, endTimestamp);

            if (expectedTimestamps.isEmpty()) {
                return new ArrayList<>();
            }

            // 3. 查询已存在的时间点列表（UTC epoch millis，已对齐到分钟起始点）
            List<Long> existingTimestamps = questDbKlineQueryService.queryExistingTimestamps(
                    symbolOnExchange, startTimestamp, endTimestamp);
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

            log.info("检测缺失的K线时间点完成: tradingPairId={}, symbol={}, 期望={}, 已存在={} (对齐后), 缺失={}", 
                    tradingPairId, symbolOnExchange, expectedTimestamps.size(), 
                    alignedExistingTimestamps.size(), missingTimestamps.size());

            return missingTimestamps;
        } catch (Exception e) {
            log.error("检测缺失的K线时间点失败: tradingPairId={}, startTimestamp={}, endTimestamp={}", 
                    tradingPairId, startTimestamp, endTimestamp, e);
            throw new RuntimeException("检测缺失K线失败: " + e.getMessage(), e);
        }
    }
}

