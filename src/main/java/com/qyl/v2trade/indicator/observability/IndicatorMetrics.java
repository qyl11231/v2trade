package com.qyl.v2trade.indicator.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 指标监控（V2：扩展支持评估Metrics）
 * 
 * <p>【V2 新增Metrics】
 * - evaluate 耗时（Timer，P50/P95/P99）
 * - 缓存命中率（Counter：cacheHits、cacheMisses）
 * - batch size 分布（Histogram）
 * 
 * <p>【原有Metrics】（保留）
 * - calc.cost_ms：指标计算耗时（V1兼容）
 * - calc.fail_count：计算失败次数
 * - calc.conflict_count：计算冲突次数
 *
 * @author qyl
 */
@Slf4j
@Component
public class IndicatorMetrics {
    
    private final MeterRegistry meterRegistry;
    
    // ========== V2 新增 Metrics ==========
    
    /**
     * evaluate 耗时（Timer，自动提供P50/P95/P99）
     */
    private Timer evaluateCostTimer;
    
    /**
     * 缓存命中次数（Counter）
     */
    private Counter cacheHits;
    
    /**
     * 缓存未命中次数（Counter）
     */
    private Counter cacheMisses;
    
    /**
     * batch size 分布（DistributionSummary）
     */
    private DistributionSummary batchSizeHistogram;
    
    /**
     * 评估失败次数（Counter）
     */
    private Counter evaluateFailures;
    
    // ========== V1 兼容 Metrics ==========
    
    /**
     * Timer for cost_ms metrics (P50/P95/P99) - V1兼容
     */
    private Timer calcCostTimer;
    
    /**
     * Counter for fail_count - V1兼容
     */
    private Counter failCounter;
    
    /**
     * Counter for conflict_count - V1兼容
     */
    private Counter conflictCounter;
    
    /**
     * Gauge for subscription_enabled_count (动态计数) - V1兼容
     */
    private final ConcurrentHashMap<String, Long> subscriptionCountMap = new ConcurrentHashMap<>();
    
    public IndicatorMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    @PostConstruct
    public void init() {
        // ========== V2 新增 Metrics 初始化 ==========
        
        // evaluate 耗时（Timer，自动提供P50/P95/P99）
        evaluateCostTimer = Timer.builder("indicator.evaluate.cost_ms")
                .description("指标评估耗时（毫秒）")
                .publishPercentiles(0.5, 0.95, 0.99) // P50, P95, P99
                .register(meterRegistry);
        
        // 缓存命中次数
        cacheHits = Counter.builder("indicator.cache.hits_total")
                .description("缓存命中次数")
                .register(meterRegistry);
        
        // 缓存未命中次数
        cacheMisses = Counter.builder("indicator.cache.misses_total")
                .description("缓存未命中次数")
                .register(meterRegistry);
        
        // batch size 分布
        batchSizeHistogram = DistributionSummary.builder("indicator.evaluate.batch_size")
                .description("批量评估大小分布")
                .register(meterRegistry);
        
        // 评估失败次数
        evaluateFailures = Counter.builder("indicator.evaluate.failures_total")
                .description("评估失败次数")
                .register(meterRegistry);
        
        // ========== V1 兼容 Metrics 初始化 ==========
        
        // 初始化Timer（自动提供P50/P95/P99等分位数）
        calcCostTimer = Timer.builder("indicator.calc.cost_ms")
                .description("指标计算耗时（毫秒）- V1兼容")
                .publishPercentiles(0.5, 0.95, 0.99) // P50, P95, P99
                .register(meterRegistry);
        
        // 初始化Counter
        failCounter = Counter.builder("indicator.calc.fail_count")
                .description("指标计算失败次数 - V1兼容")
                .register(meterRegistry);
        
        conflictCounter = Counter.builder("indicator.calc.conflict_count")
                .description("指标计算冲突次数 - V1兼容")
                .register(meterRegistry);
        
        // 注册Gauge（动态计数）
        meterRegistry.gauge("indicator.subscription.enabled_count", subscriptionCountMap,
                map -> map.values().stream().mapToLong(Long::longValue).sum());
        
        log.info("指标监控初始化完成（V2扩展）");
    }
    
    // ========== V2 新增方法 ==========
    
    /**
     * 记录评估耗时
     * 
     * @param costMs 耗时（毫秒）
     * @param indicatorCode 指标编码（用于标签）
     */
    public void recordEvaluateCost(long costMs, String indicatorCode) {
        if (evaluateCostTimer != null) {
            Timer.Sample sample = Timer.start(meterRegistry);
            sample.stop(Timer.builder("indicator.evaluate.cost_ms")
                    .description("指标评估耗时（毫秒）")
                    .tags(Tags.of("indicator_code", indicatorCode != null ? indicatorCode : "unknown"))
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(meterRegistry));
        }
    }
    
    /**
     * 记录缓存命中
     * 
     * @param indicatorCode 指标编码（用于标签）
     */
    public void recordCacheHit(String indicatorCode) {
        if (cacheHits != null) {
            Counter.builder("indicator.cache.hits_total")
                    .description("缓存命中次数")
                    .tags(Tags.of("indicator_code", indicatorCode != null ? indicatorCode : "unknown"))
                    .register(meterRegistry)
                    .increment();
        }
    }
    
    /**
     * 记录缓存未命中
     * 
     * @param indicatorCode 指标编码（用于标签）
     */
    public void recordCacheMiss(String indicatorCode) {
        if (cacheMisses != null) {
            Counter.builder("indicator.cache.misses_total")
                    .description("缓存未命中次数")
                    .tags(Tags.of("indicator_code", indicatorCode != null ? indicatorCode : "unknown"))
                    .register(meterRegistry)
                    .increment();
        }
    }
    
    /**
     * 记录批量评估大小
     * 
     * @param batchSize 批量大小
     */
    public void recordBatchSize(int batchSize) {
        if (batchSizeHistogram != null) {
            batchSizeHistogram.record(batchSize);
        }
    }
    
    /**
     * 记录评估失败
     * 
     * @param indicatorCode 指标编码（用于标签）
     */
    public void recordEvaluateFailure(String indicatorCode) {
        if (evaluateFailures != null) {
            Counter.builder("indicator.evaluate.failures_total")
                    .description("评估失败次数")
                    .tags(Tags.of("indicator_code", indicatorCode != null ? indicatorCode : "unknown"))
                    .register(meterRegistry)
                    .increment();
        }
    }
    
    // ========== V1 兼容方法 ==========
    
    /**
     * 记录计算耗时（V1兼容）
     */
    public void recordCalcCost(long costMs) {
        if (calcCostTimer != null) {
            calcCostTimer.record(costMs, TimeUnit.MILLISECONDS);
        }
    }
    
    /**
     * 记录计算失败（V1兼容）
     */
    public void recordFail(String indicatorCode, String engine) {
        if (failCounter != null) {
            Counter.builder("indicator.calc.fail_count")
                    .description("指标计算失败次数")
                    .tags(Tags.of(
                            "indicator_code", indicatorCode != null ? indicatorCode : "unknown",
                            "engine", engine != null ? engine : "unknown"
                    ))
                    .register(meterRegistry)
                    .increment();
        }
    }
    
    /**
     * 记录计算冲突（V1兼容）
     */
    public void recordConflict(String indicatorCode, String engine) {
        if (conflictCounter != null) {
            Counter.builder("indicator.calc.conflict_count")
                    .description("指标计算冲突次数")
                    .tags(Tags.of(
                            "indicator_code", indicatorCode != null ? indicatorCode : "unknown",
                            "engine", engine != null ? engine : "unknown"
                    ))
                    .register(meterRegistry)
                    .increment();
        }
    }
    
    /**
     * 更新订阅计数（V1兼容）
     */
    public void updateSubscriptionCount(String key, long count) {
        subscriptionCountMap.put(key, count);
    }
    
    /**
     * 移除订阅计数（V1兼容）
     */
    public void removeSubscriptionCount(String key) {
        subscriptionCountMap.remove(key);
    }
}

