package com.qyl.v2trade.market.calibration.trigger;

import com.qyl.v2trade.market.calibration.service.MarketCalibrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.qyl.v2trade.common.util.TimeUtil;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 补拉触发器
 * 
 * <p>统一的触发器，当发现任何缺口/不全时，调用此触发器触发补拉
 * 
 * <p>特性：
 * <ul>
 *   <li>cooldown：同一个 tradingPairId 在配置时间内最多触发一次</li>
 *   <li>inFlight：同一个 tradingPairId 同时只允许一个补拉在执行</li>
 *   <li>async：触发补拉必须异步，不能阻塞行情写入/聚合线程</li>
 *   <li>观测：记录 metrics + 结构化日志</li>
 * </ul>
 *
 * @author qyl
 */
@Slf4j
@Component
public class BackfillTrigger {

    @Autowired
    private MarketCalibrationService marketCalibrationService;

    /**
     * cooldown 时间（秒），默认300秒（5分钟）
     */
    @Value("${calibration.backfill.cooldownSeconds:300}")
    private int cooldownSeconds;

    /**
     * 回看时间（分钟），默认60分钟
     */
    @Value("${calibration.backfill.lookbackMinutes:60}")
    private int lookbackMinutes;

    /**
     * 异步线程池大小，默认4
     */
    @Value("${calibration.backfill.asyncPoolSize:4}")
    private int asyncPoolSize;

    /**
     * cooldown 记录：Key = tradingPairId, Value = 上次触发时间（毫秒）
     */
    private final ConcurrentHashMap<Long, Long> cooldownMap = new ConcurrentHashMap<>();

    /**
     * inFlight 记录：Key = tradingPairId, Value = 是否正在执行
     */
    private final ConcurrentHashMap<Long, Boolean> inFlightMap = new ConcurrentHashMap<>();

    /**
     * 异步执行线程池
     */
    private final ExecutorService asyncExecutor;

    /**
     * Metrics：触发次数
     */
    private final AtomicLong triggerCount = new AtomicLong(0);

    /**
     * Metrics：被cooldown拦截的次数
     */
    private final AtomicLong cooldownBlockedCount = new AtomicLong(0);

    /**
     * Metrics：被inFlight拦截的次数
     */
    private final AtomicLong inFlightBlockedCount = new AtomicLong(0);

    /**
     * Metrics：成功执行的次数
     */
    private final AtomicLong successCount = new AtomicLong(0);

    /**
     * Metrics：执行失败的次数
     */
    private final AtomicLong failCount = new AtomicLong(0);

    public BackfillTrigger() {
        // 创建异步线程池
        this.asyncExecutor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "BackfillTrigger-Worker");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 触发补拉最近1小时
     * 
     * @param tradingPairId 交易对ID
     * @param symbol 交易对符号
     * @param reason 触发原因（枚举值）
     * @param relatedTime 相关时间（Instant，UTC），如果为null则使用当前时间
     */
    public void triggerLast1Hour(Long tradingPairId, String symbol, BackfillReason reason, Instant relatedTime) {
        Instant now = Instant.now();
        
        // 1. 检查 cooldown
        Long lastTriggerTime = cooldownMap.get(tradingPairId);
        if (lastTriggerTime != null) {
            long elapsed = (TimeUtil.toEpochMilli(now) - lastTriggerTime) / 1000; // 秒
            if (elapsed < cooldownSeconds) {
                cooldownBlockedCount.incrementAndGet();
                log.debug("补拉触发被cooldown拦截: tradingPairId={}, symbol={}, reason={}, elapsed={}s, cooldown={}s", 
                        tradingPairId, symbol, reason, elapsed, cooldownSeconds);
                return;
            }
        }

        // 2. 检查 inFlight
        Boolean inFlight = inFlightMap.putIfAbsent(tradingPairId, Boolean.TRUE);
        if (inFlight != null && inFlight) {
            inFlightBlockedCount.incrementAndGet();
            log.debug("补拉触发被inFlight拦截: tradingPairId={}, symbol={}, reason={}", 
                    tradingPairId, symbol, reason);
            return;
        }

        // 3. 更新 cooldown
        cooldownMap.put(tradingPairId, TimeUtil.toEpochMilli(now));

        // 4. 记录触发
        triggerCount.incrementAndGet();
        
        // 5. 计算时间窗口
        // 重构：使用 Instant 进行计算，遵循时间管理约定
        Instant endTimeInstant = relatedTime != null ? relatedTime : now;
        Instant windowStartInstant = endTimeInstant.minus(lookbackMinutes, java.time.temporal.ChronoUnit.MINUTES);
        Instant windowEndInstant = endTimeInstant;

        // 6. 结构化日志
        // 重构：使用 TimeUtil 格式化日志，遵循统一时间管理规范
        log.info("触发补拉: tradingPairId={}, symbol={}, reason={}, windowStart={}, windowEnd={}", 
                tradingPairId, symbol, reason,
                TimeUtil.formatWithBothTimezones(windowStartInstant),
                TimeUtil.formatWithBothTimezones(windowEndInstant));

        // 7. 异步执行补拉（不阻塞）
        // 重构：使用 Instant 参数，遵循时间管理约定
        asyncExecutor.submit(() -> {
            try {
                marketCalibrationService.backfillLastHour(tradingPairId, windowEndInstant);
                successCount.incrementAndGet();
                log.info("补拉执行成功: tradingPairId={}, symbol={}, reason={}, windowEnd={}", 
                        tradingPairId, symbol, reason, TimeUtil.formatWithBothTimezones(windowEndInstant));
            } catch (Exception e) {
                failCount.incrementAndGet();
                log.error("补拉执行失败: tradingPairId={}, symbol={}, reason={}, windowEnd={}", 
                        tradingPairId, symbol, reason, TimeUtil.formatWithBothTimezones(windowEndInstant), e);
            } finally {
                // 释放 inFlight 标记
                inFlightMap.remove(tradingPairId);
            }
        });
    }

    /**
     * 获取统计信息
     */
    public BackfillTriggerStats getStats() {
        return new BackfillTriggerStats(
                triggerCount.get(),
                cooldownBlockedCount.get(),
                inFlightBlockedCount.get(),
                successCount.get(),
                failCount.get(),
                cooldownMap.size(),
                inFlightMap.size()
        );
    }

    /**
     * 补拉触发原因枚举
     */
    public enum BackfillReason {
        /** 1m订阅链路发现缺口 */
        GAP_DETECTED_FROM_1M,
        /** 聚合链路发现源K线不全 */
        INCOMPLETE_AGG_SOURCE
    }

    /**
     * 统计信息
     */
    public static class BackfillTriggerStats {
        private final long triggerCount;
        private final long cooldownBlockedCount;
        private final long inFlightBlockedCount;
        private final long successCount;
        private final long failCount;
        private final int cooldownMapSize;
        private final int inFlightMapSize;

        public BackfillTriggerStats(long triggerCount, long cooldownBlockedCount, long inFlightBlockedCount,
                                   long successCount, long failCount, int cooldownMapSize, int inFlightMapSize) {
            this.triggerCount = triggerCount;
            this.cooldownBlockedCount = cooldownBlockedCount;
            this.inFlightBlockedCount = inFlightBlockedCount;
            this.successCount = successCount;
            this.failCount = failCount;
            this.cooldownMapSize = cooldownMapSize;
            this.inFlightMapSize = inFlightMapSize;
        }

        public long getTriggerCount() { return triggerCount; }
        public long getCooldownBlockedCount() { return cooldownBlockedCount; }
        public long getInFlightBlockedCount() { return inFlightBlockedCount; }
        public long getSuccessCount() { return successCount; }
        public long getFailCount() { return failCount; }
        public int getCooldownMapSize() { return cooldownMapSize; }
        public int getInFlightMapSize() { return inFlightMapSize; }
    }
}

