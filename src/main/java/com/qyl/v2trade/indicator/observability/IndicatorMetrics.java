package com.qyl.v2trade.indicator.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 指标监控
 * 
 * <p>提供指标计算的监控指标：
 * - cost_ms P50/P95/P99
 * - fail_count
 * - conflict_count
 * - subscription_enabled_count
 *
 * @author qyl
 */
@Slf4j
@Component
public class IndicatorMetrics {
    
    private final MeterRegistry meterRegistry;
    
    // Timer for cost_ms metrics (P50/P95/P99)
    private Timer calcCostTimer;
    
    // Counter for fail_count
    private Counter failCounter;
    
    // Counter for conflict_count
    private Counter conflictCounter;
    
    // Gauge for subscription_enabled_count (动态计数)
    private final ConcurrentHashMap<String, Long> subscriptionCountMap = new ConcurrentHashMap<>();
    
    public IndicatorMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    @PostConstruct
    public void init() {
        // 初始化Timer（自动提供P50/P95/P99等分位数）
        calcCostTimer = Timer.builder("indicator.calc.cost_ms")
                .description("指标计算耗时（毫秒）")
                .publishPercentiles(0.5, 0.95, 0.99) // P50, P95, P99
                .register(meterRegistry);
        
        // 初始化Counter
        failCounter = Counter.builder("indicator.calc.fail_count")
                .description("指标计算失败次数")
                .register(meterRegistry);
        
        conflictCounter = Counter.builder("indicator.calc.conflict_count")
                .description("指标计算冲突次数")
                .register(meterRegistry);
        
        // 注册Gauge（动态计数）
        meterRegistry.gauge("indicator.subscription.enabled_count", subscriptionCountMap,
                map -> map.values().stream().mapToLong(Long::longValue).sum());
        
        log.info("指标监控初始化完成");
    }
    
    /**
     * 记录计算耗时
     */
    public void recordCalcCost(long costMs) {
        if (calcCostTimer != null) {
            calcCostTimer.record(costMs, TimeUnit.MILLISECONDS);
        }
    }
    
    /**
     * 记录计算失败
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
     * 记录计算冲突
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
     * 更新订阅计数
     */
    public void updateSubscriptionCount(String key, long count) {
        subscriptionCountMap.put(key, count);
    }
    
    /**
     * 移除订阅计数
     */
    public void removeSubscriptionCount(String key) {
        subscriptionCountMap.remove(key);
    }
}

