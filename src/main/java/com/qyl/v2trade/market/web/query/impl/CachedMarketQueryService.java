package com.qyl.v2trade.market.web.query.impl;

import com.qyl.v2trade.market.subscription.persistence.cache.MarketCacheService;
import com.qyl.v2trade.market.subscription.persistence.cache.impl.RedisMarketCacheService;
import com.qyl.v2trade.market.model.NormalizedKline;
import com.qyl.v2trade.market.web.query.MarketQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 带缓存的行情查询服务
 * 优先从Redis缓存查询，缓存未命中则查询QuestDB
 */
@Slf4j
@Service
@Primary
public class CachedMarketQueryService implements MarketQueryService {

    @Autowired
    @Qualifier("questDbMarketQueryService")
    private MarketQueryService questDbQueryService;

    @Autowired
    private MarketCacheService cacheService;

    // 缓存时间窗口（毫秒）- 默认查询最近1小时的数据优先从缓存获取
    private static final long CACHE_TIME_WINDOW = 60 * 60 * 1000L; // 1小时

    @Override
    public List<NormalizedKline> queryKlines(String symbol, String interval, 
                                             Long fromTimestamp, Long toTimestamp, Integer limit) {
        // 超出缓存窗口，直接从QuestDB查询
        return questDbQueryService.queryKlines(symbol, interval, fromTimestamp, toTimestamp, limit);
    }

    @Override
    public NormalizedKline queryLatestKline(String symbol, String interval) {
        // 如果缓存服务是Redis实现，检查连接状态并尝试恢复
        checkRedisConnection();
        
        // 先尝试从缓存查询（最新数据通常在缓存中）
        long now = System.currentTimeMillis();
        List<NormalizedKline> cachedKlines = cacheService.getKlinesFromCache(
            symbol, interval, now - CACHE_TIME_WINDOW, now
        );

        if (!cachedKlines.isEmpty()) {
            // 返回最新的
            return cachedKlines.get(cachedKlines.size() - 1);
        }

        // 缓存未命中，从QuestDB查询
        return questDbQueryService.queryLatestKline(symbol, interval);
    }

    @Override
    public NormalizedKline queryKlineByTimestamp(String symbol, String interval, long timestamp) {
        // 如果缓存服务是Redis实现，检查连接状态并尝试恢复
        checkRedisConnection();
        
        // 先尝试从缓存查询
        NormalizedKline cached = cacheService.getKlineFromCache(symbol, interval, timestamp);
        if (cached != null) {
            return cached;
        }

        // 缓存未命中，从QuestDB查询
        return questDbQueryService.queryKlineByTimestamp(symbol, interval, timestamp);
    }
    
    /**
     * 检查Redis连接状态并尝试恢复
     */
    private void checkRedisConnection() {
        if (cacheService instanceof RedisMarketCacheService) {
            RedisMarketCacheService redisCacheService = (RedisMarketCacheService) cacheService;
            redisCacheService.checkAndRecoverRedisConnection();
        }
    }

    /**
     * 合并并去重K线数据
     */
    private List<NormalizedKline> mergeAndDeduplicate(List<NormalizedKline> cached, 
                                                      List<NormalizedKline> db, 
                                                      Integer limit) {
        // 使用时间戳作为唯一键去重
        List<NormalizedKline> merged = new ArrayList<>();
        
        // 合并两个列表
        merged.addAll(cached);
        merged.addAll(db);

        // 按时间戳去重并排序
        List<NormalizedKline> result = merged.stream()
            .collect(Collectors.toMap(
                NormalizedKline::getTimestamp,
                k -> k,
                (k1, k2) -> k1 // 如果重复，保留第一个
            ))
            .values()
            .stream()
            .sorted((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()))
            .collect(Collectors.toList());

        // 应用limit
        if (limit != null && result.size() > limit) {
            return result.subList(0, limit);
        }

        return result;
    }
}

