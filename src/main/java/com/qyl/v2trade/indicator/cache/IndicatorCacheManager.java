package com.qyl.v2trade.indicator.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.qyl.v2trade.indicator.runtime.EvaluationContext;
import com.qyl.v2trade.indicator.runtime.IndicatorEvaluateResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 指标缓存管理器（V2新增，阶段四：完整实现）
 * 
 * <p>【职责】
 * - 生成缓存键（固定规范）
 * - 缓存查询和写入（基于 Caffeine）
 * - 缓存命中率统计
 * 
 * <p>【缓存键规范】
 * 格式：indicatorCode:version:pairId:timeframe:asOfBarTime:paramsHash
 * - paramsHash 是 params 的 SHA-256 哈希值（保证参数顺序不影响缓存键）
 * 
 * <p>【关键验收点】
 * - 关闭缓存不影响功能正确性（通过 @ConditionalOnProperty 控制）
 *
 * @author qyl
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "indicator.evaluate.cache.enabled", havingValue = "true", matchIfMissing = true)
public class IndicatorCacheManager {
    
    protected final Cache<String, IndicatorEvaluateResult> cache;
    protected final ObjectMapper objectMapper;
    protected final boolean enabled;
    
    /**
     * 公共构造函数，用于 Spring 自动注入
     */
    @Autowired
    public IndicatorCacheManager(
            @Value("${indicator.evaluate.cache.enabled:true}") boolean enabled,
            @Value("${indicator.evaluate.cache.ttl:3600}") long ttl,
            @Value("${indicator.evaluate.cache.max-size:10000}") long maxSize,
            ObjectMapper objectMapper) {
        this.enabled = enabled;
        this.objectMapper = objectMapper;
        
        if (enabled && objectMapper != null) {
            this.cache = Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofSeconds(ttl))
                    .maximumSize(maxSize)
                    .recordStats()  // 记录统计信息
                    .build();
            log.info("指标缓存已启用: TTL={}s, MaxSize={}", ttl, maxSize);
        } else {
            this.cache = null;
            log.info("指标缓存已禁用");
        }
    }
    
    @PostConstruct
    public void init() {
        if (enabled && cache != null) {
            log.info("指标缓存管理器初始化完成");
        }
    }
    
    /**
     * 生成缓存键（固定规范）
     * 
     * <p>缓存键格式：indicatorCode:version:pairId:timeframe:asOfBarTime:paramsHash
     * 
     * <p>【关键原则】
     * - 相同参数生成相同缓存键
     * - 不同参数生成不同缓存键
     * - 参数顺序不影响缓存键（Map 排序）
     * 
     * @param indicatorCode 指标编码
     * @param version 版本
     * @param context 评估上下文
     * @param params 参数
     * @return 缓存键
     */
    public String generateCacheKey(String indicatorCode, String version,
                                   EvaluationContext context,
                                   Map<String, Object> params) {
        try {
            // 1. 对 params 进行排序和序列化
            String paramsJson;
            if (params == null || params.isEmpty()) {
                paramsJson = "{}";
            } else {
                // 按 key 排序，保证参数顺序不影响缓存键
                Map<String, Object> sortedParams = params.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (v1, v2) -> v1,
                                LinkedHashMap::new
                        ));
                paramsJson = objectMapper.writeValueAsString(sortedParams);
            }
            
            // 2. 计算 params 的 hash（SHA-256）
            String paramsHash = sha256Hex(paramsJson);
            
            // 3. 拼接缓存键
            String cacheKey = String.format("%s:%s:%d:%s:%s:%s",
                    indicatorCode != null ? indicatorCode : "",
                    version != null ? version : "",
                    context.getTradingPairId() != null ? context.getTradingPairId() : 0,
                    context.getTimeframe() != null ? context.getTimeframe() : "",
                    context.getAsOfBarTime() != null ? context.getAsOfBarTime().toString() : "",
                    paramsHash);
            
            log.debug("生成缓存键: code={}, version={}, pairId={}, timeframe={}, paramsHash={}",
                    indicatorCode, version, context.getTradingPairId(), context.getTimeframe(), 
                    paramsHash.substring(0, Math.min(8, paramsHash.length())));
            
            return cacheKey;
            
        } catch (JsonProcessingException e) {
            log.error("生成缓存键失败: code={}, version={}", indicatorCode, version, e);
            // 降级：返回不包含 paramsHash 的键（不推荐，但保证功能可用）
            return String.format("%s:%s:%d:%s:%s:fallback",
                    indicatorCode, version, context.getTradingPairId(), 
                    context.getTimeframe(), context.getAsOfBarTime());
        }
    }
    
    /**
     * 查询缓存
     * 
     * @param cacheKey 缓存键
     * @return 缓存结果，不存在返回 Optional.empty()
     */
    public Optional<IndicatorEvaluateResult> get(String cacheKey) {
        if (!enabled || cache == null) {
            return Optional.empty();
        }
        
        IndicatorEvaluateResult result = cache.getIfPresent(cacheKey);
        if (result != null) {
            log.debug("缓存命中: cacheKey={}", cacheKey);
            return Optional.of(result);
        }
        
        log.debug("缓存未命中: cacheKey={}", cacheKey);
        return Optional.empty();
    }
    
    /**
     * 写入缓存
     * 
     * @param cacheKey 缓存键
     * @param result 评估结果
     */
    public void put(String cacheKey, IndicatorEvaluateResult result) {
        if (!enabled || cache == null) {
            return;
        }
        
        // 只缓存有效的结果
        if (result != null && result.isValid()) {
            cache.put(cacheKey, result);
            log.debug("写入缓存: cacheKey={}", cacheKey);
        } else {
            log.debug("跳过缓存（结果无效）: cacheKey={}, valid={}", 
                    cacheKey, result != null ? result.isValid() : false);
        }
    }
    
    /**
     * 获取缓存统计信息
     * 
     * @return 缓存统计（命中率等）
     */
    public CacheStats getStats() {
        if (!enabled || cache == null) {
            return new CacheStats(0, 0, 0.0);
        }
        
        com.github.benmanes.caffeine.cache.stats.CacheStats caffeineStats = cache.stats();
        long hits = caffeineStats.hitCount();
        long misses = caffeineStats.missCount();
        double hitRatio = (hits + misses > 0) ? (double) hits / (hits + misses) : 0.0;
        
        return new CacheStats(hits, misses, hitRatio);
    }
    
    /**
     * 计算 SHA-256 哈希值
     * 
     * @param input 输入字符串
     * @return 64位十六进制字符串
     */
    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            
            // 转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256算法不可用", e);
        }
    }
    
    /**
     * 缓存统计信息
     */
    public static class CacheStats {
        private final long hits;
        private final long misses;
        private final double hitRatio;
        
        public CacheStats(long hits, long misses, double hitRatio) {
            this.hits = hits;
            this.misses = misses;
            this.hitRatio = hitRatio;
        }
        
        public long getHits() {
            return hits;
        }
        
        public long getMisses() {
            return misses;
        }
        
        public double getHitRatio() {
            return hitRatio;
        }
        
        @Override
        public String toString() {
            return String.format("CacheStats{hits=%d, misses=%d, hitRatio=%.2f%%}", 
                    hits, misses, hitRatio * 100);
        }
    }
}

