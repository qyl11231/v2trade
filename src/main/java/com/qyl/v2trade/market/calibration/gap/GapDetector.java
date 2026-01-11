package com.qyl.v2trade.market.calibration.gap;

import com.qyl.v2trade.market.calibration.trigger.BackfillTrigger;
import com.qyl.v2trade.market.model.NormalizedKline;
import com.qyl.v2trade.market.web.query.MarketQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * K线连续性检测器
 * 
 * <p>核心逻辑：根据最新一根K线时间戳，向前检查60根K线是否连续
 * 
 * <p>检测策略：
 * <ul>
 *   <li>计算期望的60个时间戳：[currentOpenTime - 60分钟, currentOpenTime)</li>
 *   <li>从QuestDB查询这60分钟内实际存在的K线数量</li>
 *   <li>如果实际数量 < 60，触发补拉</li>
 * </ul>
 * 
 * <p>性能优化：
 * <ul>
 *   <li>乱序/重复检测（避免无效查询）</li>
 *   <li>只查询数量，不查询具体数据</li>
 * </ul>
 *
 * @author qyl
 */
@Slf4j
@Component
public class GapDetector {

    @Autowired
    private BackfillTrigger backfillTrigger;

    @Autowired(required = false)
    @Qualifier("questDbMarketQueryService")
    private MarketQueryService marketQueryService;

    /**
     * 检测窗口大小（分钟），默认60分钟
     */
    @Value("${calibration.gap.windowMinutes:60}")
    private int windowMinutes;

    /**
     * 上次查询时间：Key = tradingPairId, Value = 上次查询的最新时间戳
     * 用于乱序/重复检测，避免无效查询
     */
    private final ConcurrentHashMap<Long, Long> lastQueryTimeMap = new ConcurrentHashMap<>();

    /**
     * Metrics：检测到的缺口数量
     */
    private final AtomicLong gapDetectedCount = new AtomicLong(0);

    /**
     * Metrics：乱序/重复数量
     */
    private final AtomicLong outOfOrderCount = new AtomicLong(0);

    /**
     * Metrics：正常数量
     */
    private final AtomicLong normalCount = new AtomicLong(0);

    /**
     * Metrics：查询QuestDB次数
     */
    private final AtomicLong queryCount = new AtomicLong(0);

    /**
     * 检测缺口
     * 
     * @param tradingPairId 交易对ID
     * @param symbol 交易对符号
     * @param currentOpenTime 当前K线的openTime（毫秒，UTC epoch millis）
     */
    public void detectGap(Long tradingPairId, String symbol, long currentOpenTime) {
        try {
            // 1. 检查是否乱序/重复（当前时间戳 <= 上次查询的最新时间戳）
            Long lastQueryTime = lastQueryTimeMap.get(tradingPairId);
            if (lastQueryTime != null && currentOpenTime <= lastQueryTime) {
                outOfOrderCount.incrementAndGet();
                log.debug("检测到乱序/重复K线: tradingPairId={}, symbol={}, lastQueryTime={}, currentOpenTime={}", 
                        tradingPairId, symbol, lastQueryTime, currentOpenTime);
                return;
            }

            // 2. 计算窗口范围
            long windowStart = currentOpenTime - (windowMinutes * 60 * 1000L);
            long windowEnd = currentOpenTime;

            // 3. 从QuestDB查询实际存在的K线数量
            int actualCount = queryActualCount(symbol, windowStart, windowEnd);
            queryCount.incrementAndGet();

            // 4. 判断是否够60根
            int expectedCount = windowMinutes; // 期望60根
            if (actualCount < expectedCount) {
                gapDetectedCount.incrementAndGet();
                log.warn("检测到K线不连续: tradingPairId={}, symbol={}, windowStart={}, windowEnd={}, " +
                        "expectedCount={}, actualCount={}, missingCount={}", 
                        tradingPairId, symbol, windowStart, windowEnd,
                        expectedCount, actualCount, expectedCount - actualCount);
                
                // 触发补拉（具体缺失哪些时间戳，由补拉层去处理）
                backfillTrigger.triggerLast1Hour(
                        tradingPairId, 
                        symbol, 
                        BackfillTrigger.BackfillReason.GAP_DETECTED_FROM_1M, 
                        currentOpenTime
                );
            } else {
                normalCount.incrementAndGet();
            }

            // 5. 更新查询缓存
            lastQueryTimeMap.put(tradingPairId, currentOpenTime);

        } catch (Exception e) {
            log.error("缺口检测异常: tradingPairId={}, symbol={}, currentOpenTime={}", 
                    tradingPairId, symbol, currentOpenTime, e);
        }
    }

    /**
     * 从QuestDB查询实际存在的K线数量
     * 
     * @param symbol 交易对符号
     * @param windowStart 窗口开始时间
     * @param windowEnd 窗口结束时间
     * @return 实际存在的K线数量
     */
    private int queryActualCount(String symbol, long windowStart, long windowEnd) {
        if (marketQueryService == null) {
            log.debug("MarketQueryService未配置，返回0");
            return 0;
        }

        try {
            // 从QuestDB查询1m K线数据
            List<NormalizedKline> klines = marketQueryService.queryKlines(
                    symbol, 
                    "1m", 
                    windowStart, 
                    windowEnd, 
                    null // 不限制数量，查询所有
            );



            return klines.size();

        } catch (Exception e) {
            log.error("查询实际K线数量失败: symbol={}, windowStart={}, windowEnd={}", 
                    symbol, windowStart, windowEnd, e);
            return 0;
        }
    }

    /**
     * 获取统计信息
     */
    public GapDetectorStats getStats() {
        return new GapDetectorStats(
                gapDetectedCount.get(),
                outOfOrderCount.get(),
                normalCount.get(),
                queryCount.get(),
                lastQueryTimeMap.size()
        );
    }

    /**
     * 统计信息
     */
    public static class GapDetectorStats {
        private final long gapDetectedCount;
        private final long outOfOrderCount;
        private final long normalCount;
        private final long queryCount;
        private final int trackedPairCount;

        public GapDetectorStats(long gapDetectedCount, long outOfOrderCount, 
                               long normalCount, long queryCount, int trackedPairCount) {
            this.gapDetectedCount = gapDetectedCount;
            this.outOfOrderCount = outOfOrderCount;
            this.normalCount = normalCount;
            this.queryCount = queryCount;
            this.trackedPairCount = trackedPairCount;
        }

        public long getGapDetectedCount() { return gapDetectedCount; }
        public long getOutOfOrderCount() { return outOfOrderCount; }
        public long getNormalCount() { return normalCount; }
        public long getQueryCount() { return queryCount; }
        public int getTrackedPairCount() { return trackedPairCount; }
    }
}
