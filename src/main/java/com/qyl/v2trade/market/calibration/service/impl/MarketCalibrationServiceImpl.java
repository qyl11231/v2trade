package com.qyl.v2trade.market.calibration.service.impl;

import com.qyl.v2trade.market.calibration.service.*;
import com.qyl.v2trade.market.calibration.service.dto.TradingPairInfo;
import com.qyl.v2trade.market.model.NormalizedKline;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * 市场校准服务实现类
 * 
 * <p>提供统一的补拉能力，可被定时任务和BackfillTrigger复用
 *
 * @author qyl
 */
@Slf4j
@Service
public class MarketCalibrationServiceImpl implements MarketCalibrationService {

    @Autowired
    private TradingPairInfoService tradingPairInfoService;

    @Autowired
    private KlineGapDetector klineGapDetector;

    @Autowired
    private HistoricalKlineFetcher historicalKlineFetcher;

    @Autowired
    private KlineDataFiller klineDataFiller;

    /**
     * 回看时间（分钟），默认60分钟
     */
    @Value("${calibration.backfill.lookbackMinutes:60}")
    private int lookbackMinutes;

    @Override
    public void backfillLastHour(Long tradingPairId, long endTimeUtc) {
        try {
            log.info("开始补拉最近1小时数据: tradingPairId={}, endTimeUtc={}", tradingPairId, endTimeUtc);

            // 1. 获取交易对信息
            TradingPairInfo pairInfo = tradingPairInfoService.getTradingPairInfo(tradingPairId);
            String symbolOnExchange = pairInfo.getSymbolOnExchange();
            String symbol = pairInfo.getSymbol();

            // 2. 计算时间窗口
            long windowStart = endTimeUtc - (lookbackMinutes * 60 * 1000L);
            long windowEnd = endTimeUtc;

            ZonedDateTime windowStartUtc = Instant.ofEpochMilli(windowStart).atZone(ZoneId.of("UTC"));
            ZonedDateTime windowEndUtc = Instant.ofEpochMilli(windowEnd).atZone(ZoneId.of("UTC"));

            log.info("补拉时间窗口: tradingPairId={}, symbol={}, windowStart={}, windowEnd={}, " +
                    "windowStartUtc={}, windowEndUtc={}", 
                    tradingPairId, symbol, windowStart, windowEnd, windowStartUtc, windowEndUtc);

            // 3. 检测缺失的时间点
            List<Long> missingTimestamps = klineGapDetector.detectMissingTimestamps(
                    tradingPairId, windowStart, windowEnd);

            if (missingTimestamps.isEmpty()) {
                log.info("补拉完成（无缺失数据）: tradingPairId={}, symbol={}", tradingPairId, symbol);
                return;
            }

            log.info("检测到缺失时间点: tradingPairId={}, symbol={}, 缺失数量={}", 
                    tradingPairId, symbol, missingTimestamps.size());

            // 4. 从OKX API拉取数据
            List<NormalizedKline> klines = historicalKlineFetcher.fetchHistoricalKlines(
                    symbolOnExchange, missingTimestamps);
            log.info("从OKX API拉取K线数据: tradingPairId={}, symbol={}, 拉取数量={}", 
                    tradingPairId, symbol, klines.size());

            if (klines.isEmpty()) {
                log.warn("补拉完成（API返回空）: tradingPairId={}, symbol={}", tradingPairId, symbol);
                return;
            }

            // 5. 插入到QuestDB（幂等，允许重复写但不会造成脏数据/重复数据）
            int filledCount = klineDataFiller.fillMissingKlines(symbolOnExchange, klines);
            log.info("补拉完成: tradingPairId={}, symbol={}, 缺失={}, 拉取={}, 插入={}", 
                    tradingPairId, symbol, missingTimestamps.size(), klines.size(), filledCount);

        } catch (Exception e) {
            log.error("补拉最近1小时数据失败: tradingPairId={}, endTimeUtc={}", tradingPairId, endTimeUtc, e);
            throw new RuntimeException("补拉失败: " + e.getMessage(), e);
        }
    }
}

