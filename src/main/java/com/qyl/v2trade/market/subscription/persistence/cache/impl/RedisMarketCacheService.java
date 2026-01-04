package com.qyl.v2trade.market.subscription.persistence.cache.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qyl.v2trade.market.subscription.persistence.cache.MarketCacheService;
import com.qyl.v2trade.market.model.NormalizedKline;
import io.lettuce.core.RedisConnectionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis行情缓存服务实现
 */
@Slf4j
@Service
public class RedisMarketCacheService implements MarketCacheService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${market.data.cache.key-prefix:market:kline:}")
    private String keyPrefix;
    
    // Redis是否可用标志
    private volatile boolean redisAvailable = true;
    
    // 降级计数器
    private volatile int failureCount = 0;
    private static final int MAX_FAILURE_COUNT = 5; // 最大失败次数
    private static final int FAILURE_RESET_THRESHOLD = 10; // 重置失败计数的阈值

    @Override
    public void cacheKline(NormalizedKline kline, int cacheDurationMinutes) {
        if (!redisAvailable) {
            log.debug("Redis不可用，跳过缓存: symbol={}, timestamp={}", kline.getSymbol(), kline.getTimestamp());
            return;
        }
        
        try {
            String key = buildKey(kline.getSymbol(), kline.getInterval(), kline.getTimestamp());
            String value = objectMapper.writeValueAsString(kline);
            
            redisTemplate.opsForValue().set(key, value, cacheDurationMinutes, TimeUnit.MINUTES);
            log.debug("缓存K线: key={}", key);
            
            // 重置失败计数
            if (failureCount > 0) {
                failureCount = 0;
            }
        } catch (RedisConnectionException e) {
            handleRedisConnectionFailure("缓存K线", kline.getSymbol(), kline.getTimestamp(), e);
        } catch (Exception e) {
            log.error("缓存K线失败: symbol={}, timestamp={}", kline.getSymbol(), kline.getTimestamp(), e);
        }
    }

    @Override
    public NormalizedKline getKlineFromCache(String symbol, String interval, long timestamp) {
        if (!redisAvailable) {
            log.debug("Redis不可用，跳过缓存查询: symbol={}, timestamp={}", symbol, timestamp);
            return null;
        }
        
        try {
            String key = buildKey(symbol, interval, timestamp);
            String value = redisTemplate.opsForValue().get(key);
            
            if (value == null) {
                return null;
            }
            
            NormalizedKline result = objectMapper.readValue(value, NormalizedKline.class);
            
            // 重置失败计数
            if (failureCount > 0) {
                failureCount = 0;
            }
            
            return result;
        } catch (RedisConnectionException e) {
            handleRedisConnectionFailure("获取K线", symbol, timestamp, e);
            return null;
        } catch (Exception e) {
            log.error("从缓存获取K线失败: symbol={}, timestamp={}", symbol, timestamp, e);
            return null;
        }
    }

    @Override
    public List<NormalizedKline> getKlinesFromCache(String symbol, String interval, 
                                                     long fromTimestamp, long toTimestamp) {
        List<NormalizedKline> result = new ArrayList<>();
        
        if (!redisAvailable) {
            log.debug("Redis不可用，跳过缓存查询: symbol={}, interval={}", symbol, interval);
            return result;
        }
        
        try {
            // 构建key模式
            String pattern = buildKeyPattern(symbol, interval);
            
            // 获取所有匹配的key
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys == null || keys.isEmpty()) {
                return result;
            }
            
            // 过滤时间范围并获取值
            for (String key : keys) {
                try {
                    // 从key中提取时间戳
                    long timestamp = extractTimestampFromKey(key);
                    if (timestamp >= fromTimestamp && timestamp <= toTimestamp) {
                        String value = redisTemplate.opsForValue().get(key);
                        if (value != null) {
                            NormalizedKline kline = objectMapper.readValue(value, NormalizedKline.class);
                            result.add(kline);
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析缓存key失败: {}", key, e);
                }
            }
            
            // 按时间戳排序
            result.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
            
            // 重置失败计数
            if (failureCount > 0) {
                failureCount = 0;
            }
            
        } catch (RedisConnectionException e) {
            handleRedisConnectionFailure("获取K线列表", symbol, interval, e);
        } catch (Exception e) {
            log.error("从缓存获取K线列表失败: symbol={}, interval={}", symbol, interval, e);
        }
        
        return result;
    }

    @Override
    public void clearCache(String symbol, String interval) {
        if (!redisAvailable) {
            log.debug("Redis不可用，跳过清除缓存: symbol={}, interval={}", symbol, interval);
            return;
        }
        
        try {
            String pattern = buildKeyPattern(symbol, interval);
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("清除缓存: symbol={}, interval={}, count={}", symbol, interval, keys.size());
                
                // 重置失败计数
                if (failureCount > 0) {
                    failureCount = 0;
                }
            }
        } catch (RedisConnectionException e) {
            handleRedisConnectionFailure("清除缓存", symbol, interval, e);
        } catch (Exception e) {
            log.error("清除缓存失败: symbol={}, interval={}", symbol, interval, e);
        }
    }

    /**
     * 构建缓存key
     */
    private String buildKey(String symbol, String interval, long timestamp) {
        return keyPrefix + symbol + ":" + interval + ":" + timestamp;
    }

    /**
     * 构建key模式（用于模糊查询）
     */
    private String buildKeyPattern(String symbol, String interval) {
        return keyPrefix + symbol + ":" + interval + ":*";
    }

    /**
     * 从key中提取时间戳
     */
    private long extractTimestampFromKey(String key) {
        try {
            String[] parts = key.split(":");
            if (parts.length > 0) {
                return Long.parseLong(parts[parts.length - 1]);
            }
        } catch (Exception e) {
            log.warn("从key提取时间戳失败: {}", key, e);
        }
        return 0;
    }
    
    /**
     * 处理Redis连接失败
     */
    private void handleRedisConnectionFailure(String operation, Object... params) {
        failureCount++;
        log.error("Redis连接失败 (第{}次): {} - params={}", failureCount, operation, params);
        
        if (failureCount >= MAX_FAILURE_COUNT) {
            redisAvailable = false;
            log.warn("Redis连接不可用，已切换到降级模式，后续操作将跳过Redis");
        }
    }
    
    /**
     * 检查Redis连接状态并尝试恢复
     */
    public boolean checkAndRecoverRedisConnection() {
        if (redisAvailable) {
            return true; // 如果Redis可用，直接返回
        }
        
        try {
            // 尝试执行一个简单的ping操作来检查连接
            redisTemplate.opsForValue().get("ping_test");
            
            // 如果没有抛出异常，说明Redis已恢复
            redisAvailable = true;
            failureCount = 0;
            log.info("Redis连接已恢复");
            return true;
        } catch (RedisConnectionException e) {
            log.debug("Redis连接仍不可用: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            // 其他异常也说明连接有问题
            log.debug("检查Redis连接时发生异常: {}", e.getMessage());
            return false;
        }
    }
}

