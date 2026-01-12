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

import java.time.Instant;
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
                                             Instant fromTime, Instant toTime, Integer limit) {
        // 重构：按照时间管理约定，直接传递 Instant 参数
        // 超出缓存窗口，直接从QuestDB查询
        return questDbQueryService.queryKlines(symbol, interval, fromTime, toTime, limit);
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
    public NormalizedKline queryKlineByTimestamp(String symbol, String interval, Instant timestamp) {
        // 重构：按照时间管理约定，使用 Instant 作为参数类型
        // 如果缓存服务是Redis实现，检查连接状态并尝试恢复
        checkRedisConnection();
        
        // 先尝试从缓存查询（需要转换为 long 用于缓存查询）
        // 注意：缓存服务接口仍使用 long，这是缓存层的边界转换
        long timestampMillis = timestamp.toEpochMilli();
        NormalizedKline cached = cacheService.getKlineFromCache(symbol, interval, timestampMillis);
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

