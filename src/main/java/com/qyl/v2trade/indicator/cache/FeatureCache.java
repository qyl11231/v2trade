package com.qyl.v2trade.indicator.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 特征缓存（Phase 2，可选性能优化）
 * 
 * <p>【⚠️ 重要定位】
 * > **FeatureCache 不得作为功能正确性的依赖，只能作为性能优化手段。**
 * 
 * <p>【实施原则】
 * - ✅ **MVP 阶段**：必须不依赖 FeatureCache，功能完全可用
 * - ✅ **Phase 2 阶段**：压测后 CPU 不达标时，作为性能增强项引入
 * - ✅ **限制范围**：只允许 cache 纯函数特征（SMA、EMA、TR、最高低等基础序列）
 * - ❌ **严禁**：
 *   - 将业务逻辑塞进 FeatureCache
 *   - 将 FeatureCache 作为功能正确性的前提条件
 *   - 在 MVP 阶段强制依赖 FeatureCache
 * 
 * <p>【缓存范围】
 * - 只缓存纯函数特征（基础序列）
 * - 例如：SMA、EMA、TR、最高价、最低价等
 * - 不缓存复杂指标（如MACD、RSI等）
 * 
 * <p>【缓存键设计】
 * - 格式：`feature:${featureType}:${pairId}:${timeframe}:${params}:${asOfBarTime}`
 * - 例如：`feature:sma:1:1h:period=20:2024-01-01T12:00:00`
 *
 * @author qyl
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "indicator.feature.cache.enabled", havingValue = "true", matchIfMissing = false)
public class FeatureCache {
    
    /**
     * 特征缓存（key: cacheKey, value: 特征值列表）
     */
    private final Cache<String, List<BigDecimal>> cache;
    
    private final boolean enabled;
    
    /**
     * 支持的特征类型（纯函数）
     */
    private static final String[] SUPPORTED_FEATURES = {
        "sma", "ema", "tr", "high", "low", "close", "open", "volume"
    };
    
    public FeatureCache(
            @Value("${indicator.feature.cache.ttl:1800}") long ttl,
            @Value("${indicator.feature.cache.max-size:5000}") long maxSize) {
        this.enabled = true;
        
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(ttl))
                .maximumSize(maxSize)
                .recordStats()
                .build();
        
        log.info("特征缓存已启用: TTL={}s, MaxSize={}", ttl, maxSize);
    }
    
    /**
     * 生成特征缓存键
     * 
     * @param featureType 特征类型（如：sma、ema、tr）
     * @param pairId 交易对ID
     * @param timeframe 周期
     * @param params 参数（如：{"period": 20}）
     * @param asOfBarTime Bar时间
     * @return 缓存键
     */
    public String generateCacheKey(String featureType, Long pairId, String timeframe,
                                   Map<String, Object> params, LocalDateTime asOfBarTime) {
        // 构建参数字符串
        StringBuilder paramsStr = new StringBuilder();
        if (params != null && !params.isEmpty()) {
            params.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> paramsStr.append(e.getKey()).append("=").append(e.getValue()).append(","));
        }
        
        return String.format("feature:%s:%d:%s:%s:%s",
                featureType, pairId, timeframe, paramsStr.toString(), asOfBarTime);
    }
    
    /**
     * 查询特征缓存
     * 
     * @param cacheKey 缓存键
     * @return 特征值列表，不存在返回 Optional.empty()
     */
    public Optional<List<BigDecimal>> get(String cacheKey) {
        if (!enabled || cache == null) {
            return Optional.empty();
        }
        
        List<BigDecimal> result = cache.getIfPresent(cacheKey);
        if (result != null) {
            log.debug("特征缓存命中: cacheKey={}", cacheKey);
            return Optional.of(result);
        }
        
        return Optional.empty();
    }
    
    /**
     * 写入特征缓存
     * 
     * @param cacheKey 缓存键
     * @param values 特征值列表
     */
    public void put(String cacheKey, List<BigDecimal> values) {
        if (!enabled || cache == null) {
            return;
        }
        
        if (values != null && !values.isEmpty()) {
            cache.put(cacheKey, values);
            log.debug("写入特征缓存: cacheKey={}, size={}", cacheKey, values.size());
        }
    }
    
    /**
     * 检查特征类型是否支持缓存
     * 
     * @param featureType 特征类型
     * @return 是否支持
     */
    public boolean isSupported(String featureType) {
        if (featureType == null) {
            return false;
        }
        
        for (String supported : SUPPORTED_FEATURES) {
            if (supported.equalsIgnoreCase(featureType)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 获取缓存统计信息
     * 
     * @return 缓存统计
     */
    public com.github.benmanes.caffeine.cache.stats.CacheStats getStats() {
        if (!enabled || cache == null) {
            return null;
        }
        
        return cache.stats();
    }
}

