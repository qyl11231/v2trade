package com.qyl.v2trade.business.strategy.decision.guard;

import com.qyl.v2trade.business.strategy.decision.context.DecisionContext;
import com.qyl.v2trade.business.strategy.decision.context.snapshot.BarSnapshot;
import com.qyl.v2trade.business.strategy.decision.context.snapshot.IndicatorSnapshot;
import com.qyl.v2trade.business.strategy.decision.context.snapshot.SignalSnapshot;
import com.qyl.v2trade.business.strategy.decision.event.PriceTriggeredDecisionEvent;
import com.qyl.v2trade.common.constants.DecisionTriggerTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 去重门禁
 * 
 * <p>职责：
 * <ul>
 *   <li>防止同一触发源重复触发决策</li>
 *   <li>使用Redis缓存（降级到内存缓存）</li>
 *   <li>根据触发类型构建不同的去重键</li>
 * </ul>
 * 
 * <p>阶段2约束：
 * <ul>
 *   <li>同一signalIntentId只触发一次</li>
 *   <li>同一指标+barTime只触发一次</li>
 *   <li>同一barCloseTime只触发一次</li>
 *   <li>同一价格触发类型在cooldown窗口内只触发一次</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DedupGate implements Gate {

    private final StringRedisTemplate redisTemplate;
    private final DedupCacheFallback fallbackCache;

    /**
     * 价格触发cooldown时间（秒），默认5秒
     */
    @Value("${strategy.decision.guard.price.cooldown-seconds:5}")
    private int priceCooldownSeconds;

    private static final String REDIS_KEY_PREFIX = "strategy:decision:dedup:";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    public String getName() {
        return "DedupGate";
    }

    @Override
    public GuardResult check(DecisionContext ctx) {
        if (ctx == null) {
            return GuardResult.rejected(getName(), "决策上下文为空");
        }

        // 构建去重键
        String dedupKey = buildDedupKey(ctx);
        if (dedupKey == null) {
            log.warn("无法构建去重键，跳过去重校验: strategyId={}, tradingPairId={}",
                ctx.getStrategyId(), ctx.getTradingPairId());
            return GuardResult.allowed();  // 无法构建去重键时允许通过
        }

        // 检查是否重复
        boolean isDuplicate = isDuplicate(dedupKey, ctx.getTriggerType());
        if (isDuplicate) {
            return GuardResult.rejected(getName(), "重复触发: dedupKey=" + dedupKey);
        }

        // 标记为已处理
        markAsProcessed(dedupKey, ctx.getTriggerType());

        log.debug("去重门禁通过: strategyId={}, tradingPairId={}, dedupKey={}",
            ctx.getStrategyId(), ctx.getTradingPairId(), dedupKey);

        return GuardResult.allowed();
    }

    /**
     * 构建去重键
     * 
     * <p>格式：{triggerType}:{strategyId}:{tradingPairId}:{uniqueKey}
     */
    private String buildDedupKey(DecisionContext ctx) {
        DecisionTriggerTypeEnum triggerType = ctx.getTriggerType();
        Long strategyId = ctx.getStrategyId();
        Long tradingPairId = ctx.getTradingPairId();

        if (strategyId == null || tradingPairId == null) {
            return null;
        }

        String uniqueKey;
        switch (triggerType) {
            case SIGNAL:
                uniqueKey = buildSignalDedupKey(ctx);
                break;
            case INDICATOR:
                uniqueKey = buildIndicatorDedupKey(ctx);
                break;
            case BAR:
                uniqueKey = buildBarDedupKey(ctx);
                break;
            case PRICE:
                uniqueKey = buildPriceDedupKey(ctx);
                break;
            default:
                return null;
        }

        if (uniqueKey == null) {
            return null;
        }

        return String.format("%s:%d:%d:%s", triggerType.getCode(), strategyId, tradingPairId, uniqueKey);
    }

    /**
     * 构建信号去重键
     * 
     * <p>格式：{signalIntentId}
     */
    private String buildSignalDedupKey(DecisionContext ctx) {
        SignalSnapshot signal = ctx.getSignalSnapshot();
        if (signal == null || signal.getSignalIntentId() == null) {
            return null;
        }
        return String.valueOf(signal.getSignalIntentId());
    }

    /**
     * 构建指标去重键
     * 
     * <p>格式：{indicatorCode}:{barTime}
     */
    private String buildIndicatorDedupKey(DecisionContext ctx) {
        IndicatorSnapshot indicator = ctx.getIndicatorSnapshot();
        if (indicator == null || indicator.getBarTime() == null) {
            return null;
        }
        return String.format("%s:%s", indicator.getIndicatorCode(),
            indicator.getBarTime().format(DATE_TIME_FORMATTER));
    }

    /**
     * 构建K线去重键
     * 
     * <p>格式：{timeframe}:{barCloseTime}
     */
    private String buildBarDedupKey(DecisionContext ctx) {
        BarSnapshot bar = ctx.getBarSnapshot();
        if (bar == null || bar.getBarCloseTime() == null) {
            return null;
        }
        return String.format("%s:%s", bar.getTimeframe(),
            bar.getBarCloseTime().format(DATE_TIME_FORMATTER));
    }

    /**
     * 构建价格去重键
     * 
     * <p>格式：{triggerType}:{cooldownWindow}
     * 
     * <p>cooldownWindow = triggeredAt向下取整到cooldown周期
     */
    private String buildPriceDedupKey(DecisionContext ctx) {
        Object triggerEvent = ctx.getTriggerEvent();
        if (!(triggerEvent instanceof PriceTriggeredDecisionEvent)) {
            return null;
        }

        PriceTriggeredDecisionEvent event = (PriceTriggeredDecisionEvent) triggerEvent;
        LocalDateTime triggeredAt = event.getTriggeredAt();
        if (triggeredAt == null) {
            return null;
        }

        // 计算cooldown窗口（向下取整到cooldown周期）
        long seconds = triggeredAt.toEpochSecond(java.time.ZoneOffset.UTC);
        long cooldownWindow = (seconds / priceCooldownSeconds) * priceCooldownSeconds;
        LocalDateTime cooldownWindowTime = LocalDateTime.ofEpochSecond(cooldownWindow, 0,
            java.time.ZoneOffset.UTC);

        return String.format("%s:%s", event.getTriggerType(),
            cooldownWindowTime.format(DATE_TIME_FORMATTER));
    }

    /**
     * 检查是否重复
     */
    private boolean isDuplicate(String dedupKey, DecisionTriggerTypeEnum triggerType) {
        String cacheKey = REDIS_KEY_PREFIX + dedupKey;

        try {
            // 尝试从Redis读取
            String existing = redisTemplate.opsForValue().get(cacheKey);
            if (existing != null && !existing.isEmpty()) {
                return true;  // 已存在，重复
            }
        } catch (Exception e) {
            log.warn("Redis读取失败，降级到内存缓存: dedupKey={}", dedupKey, e);
            // 降级到内存缓存
            return fallbackCache.exists(cacheKey);
        }

        return false;
    }

    /**
     * 标记为已处理
     */
    private void markAsProcessed(String dedupKey, DecisionTriggerTypeEnum triggerType) {
        String cacheKey = REDIS_KEY_PREFIX + dedupKey;
        long ttl = getTtl(triggerType);

        try {
            // 尝试写入Redis
            redisTemplate.opsForValue().set(cacheKey, String.valueOf(System.currentTimeMillis()), ttl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis写入失败，降级到内存缓存: dedupKey={}", dedupKey, e);
            // 降级到内存缓存
            fallbackCache.put(cacheKey, ttl);
        }
    }

    /**
     * 获取TTL（秒）
     */
    private long getTtl(DecisionTriggerTypeEnum triggerType) {
        switch (triggerType) {
            case SIGNAL:
                return 3600;  // 1小时
            case INDICATOR:
                return 3600;  // 1小时
            case BAR:
                return 86400;  // 24小时
            case PRICE:
                return priceCooldownSeconds + 1;  // cooldown + 1秒
            default:
                return 3600;
        }
    }

    /**
     * 内存缓存降级（Redis不可用时使用）
     */
    @Component
    public static class DedupCacheFallback {
        private final java.util.concurrent.ConcurrentHashMap<String, Long> cache = new java.util.concurrent.ConcurrentHashMap<>();
        private static final long CLEANUP_INTERVAL_MS = 60000;  // 1分钟清理一次

        public DedupCacheFallback() {
            // 启动清理线程
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(this::cleanup, CLEANUP_INTERVAL_MS, CLEANUP_INTERVAL_MS,
                    java.util.concurrent.TimeUnit.MILLISECONDS);
        }

        public boolean exists(String key) {
            Long expireTime = cache.get(key);
            if (expireTime == null) {
                return false;
            }
            if (System.currentTimeMillis() > expireTime) {
                cache.remove(key);
                return false;
            }
            return true;
        }

        public void put(String key, long ttlSeconds) {
            long expireTime = System.currentTimeMillis() + (ttlSeconds * 1000);
            cache.put(key, expireTime);
        }

        private void cleanup() {
            long now = System.currentTimeMillis();
            cache.entrySet().removeIf(entry -> entry.getValue() < now);
        }
    }
}

