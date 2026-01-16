package com.qyl.v2trade.business.strategy.runtime.dedup;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine 实现的去重器
 * 
 * <p>使用 Caffeine 缓存实现 30 秒 TTL 去重
 *
 * @author qyl
 */
@Component
public class CaffeineTriggerDeduplicator implements TriggerDeduplicator {
    
    private final Cache<String, Boolean> dedupCache;
    
    public CaffeineTriggerDeduplicator() {
        this.dedupCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .maximumSize(100_000)  // 防止内存无限增长
            .build();
    }
    
    @Override
    public boolean shouldProcess(String eventKey) {
        // 如果已存在，返回 false（跳过）
        if (dedupCache.getIfPresent(eventKey) != null) {
            return false;
        }
        
        // 第一次遇到，标记并返回 true
        dedupCache.put(eventKey, true);
        return true;
    }
}

