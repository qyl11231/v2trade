package com.qyl.v2trade.market.aggregation.persistence.impl;

import com.qyl.v2trade.market.aggregation.event.AggregatedKLine;
import com.qyl.v2trade.market.aggregation.persistence.AggregatedKLineStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * QuestDB聚合K线存储服务实现
 * 
 * <p>负责将聚合完成的K线数据持久化到QuestDB
 *
 * @author qyl
 */
@Slf4j
@Service
public class QuestDbAggregatedKLineStorageServiceImpl implements AggregatedKLineStorageService {
    
    @Autowired
    @Qualifier("questDbJdbcTemplate")
    private JdbcTemplate questDbJdbcTemplate;
    
    /**
     * 统计信息：总写入次数
     */
    private final AtomicLong totalWriteCount = new AtomicLong(0);
    
    /**
     * 统计信息：成功写入次数
     */
    private final AtomicLong successWriteCount = new AtomicLong(0);
    
    /**
     * 统计信息：跳过写入次数（已存在）
     */
    private final AtomicLong skipWriteCount = new AtomicLong(0);
    
    /**
     * 统计信息：写入失败次数
     */
    private final AtomicLong failWriteCount = new AtomicLong(0);
    
    /**
     * 根据周期获取表名
     */
    private String getTableName(String period) {
        return "kline_" + period;
    }
    
    /**
     * 生成INSERT SQL语句
     */
    private String getInsertSql(String period) {
        String tableName = getTableName(period);
        return "INSERT INTO " + tableName + " (symbol, ts, open, high, low, close, volume, source_kline_count) " +
               "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    }
    
    /**
     * 生成EXISTS SQL语句
     */
    private String getExistsSql(String period) {
        String tableName = getTableName(period);
        return "SELECT COUNT(*) FROM " + tableName + " WHERE symbol = ? AND ts = ?";
    }
    
    @Override
    public boolean save(AggregatedKLine aggregatedKLine) {
        return saveWithRetry(aggregatedKLine, 3);
    }
    
    /**
     * 带重试的写入方法
     */
    private boolean saveWithRetry(AggregatedKLine aggregatedKLine, int maxRetries) {
        totalWriteCount.incrementAndGet();
        
        // 【重要】写入前检查：如果已存在，跳过写入（保证数据唯一性）
        if (exists(aggregatedKLine.symbol(), aggregatedKLine.period(), aggregatedKLine.timestamp())) {
            skipWriteCount.incrementAndGet();
            log.debug("聚合K线已存在，跳过写入: symbol={}, period={}, timestamp={}", 
                    aggregatedKLine.symbol(), aggregatedKLine.period(), aggregatedKLine.timestamp());
            return false;
        }
        
        // 执行写入（带重试）
        int retryCount = 0;
        while (retryCount < maxRetries) {
            try {
                boolean success = doSave(aggregatedKLine);
                if (success) {
                    successWriteCount.incrementAndGet();
                    return true;
                } else {
                    retryCount++;
                    if (retryCount < maxRetries) {
                        log.warn("写入失败，重试 {}/{}: symbol={}, period={}, timestamp={}", 
                                retryCount, maxRetries, aggregatedKLine.symbol(), 
                                aggregatedKLine.period(), aggregatedKLine.timestamp());
                        try {
                            Thread.sleep(100 * retryCount); // 递增延迟
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                retryCount++;
                if (retryCount < maxRetries) {
                    log.warn("写入异常，重试 {}/{}: symbol={}, period={}, timestamp={}", 
                            retryCount, maxRetries, aggregatedKLine.symbol(), 
                            aggregatedKLine.period(), aggregatedKLine.timestamp(), e);
                    try {
                        Thread.sleep(100 * retryCount);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    log.error("写入失败（已重试{}次）: symbol={}, period={}, timestamp={}", 
                            maxRetries, aggregatedKLine.symbol(), aggregatedKLine.period(), 
                            aggregatedKLine.timestamp(), e);
                    failWriteCount.incrementAndGet();
                }
            }
        }
        
        return false;
    }
    
    /**
     * 执行实际的写入操作
     */
    private boolean doSave(AggregatedKLine aggregatedKLine) {
        try {
            // 时间戳语义：epoch millis (UTC)，转换为Instant后写入QuestDB TIMESTAMP (UTC)
            Instant timestampInstant = Instant.ofEpochMilli(aggregatedKLine.timestamp());
            Timestamp timestamp = Timestamp.from(timestampInstant);
            
            String insertSql = getInsertSql(aggregatedKLine.period());
            
            int rows = questDbJdbcTemplate.update(insertSql,
                    aggregatedKLine.symbol(),
                    timestamp,
                    aggregatedKLine.open().doubleValue(),
                    aggregatedKLine.high().doubleValue(),
                    aggregatedKLine.low().doubleValue(),
                    aggregatedKLine.close().doubleValue(),
                    aggregatedKLine.volume().doubleValue(),
                    aggregatedKLine.sourceKlineCount()
            );
            
            if (rows > 0) {
                log.debug("聚合K线已保存到QuestDB: symbol={}, period={}, timestamp={}, sourceKlineCount={}", 
                        aggregatedKLine.symbol(), aggregatedKLine.period(), 
                        aggregatedKLine.timestamp(), aggregatedKLine.sourceKlineCount());
                return true;
            } else {
                log.warn("聚合K线保存失败（返回0行）: symbol={}, period={}, timestamp={}", 
                        aggregatedKLine.symbol(), aggregatedKLine.period(), aggregatedKLine.timestamp());
                return false;
            }
        } catch (Exception e) {
            log.error("保存聚合K线异常: symbol={}, period={}, timestamp={}", 
                    aggregatedKLine.symbol(), aggregatedKLine.period(), aggregatedKLine.timestamp(), e);
            throw e; // 重新抛出异常，让重试逻辑处理
        }
    }
    
    /**
     * 异步写入（不阻塞聚合流程）
     */
    @Async
    public CompletableFuture<Boolean> saveAsync(AggregatedKLine aggregatedKLine) {
        boolean result = save(aggregatedKLine);
        return CompletableFuture.completedFuture(result);
    }
    
    @Override
    public int batchSave(List<AggregatedKLine> aggregatedKLines) {
        if (aggregatedKLines == null || aggregatedKLines.isEmpty()) {
            return 0;
        }
        
        AtomicInteger successCount = new AtomicInteger(0);
        for (AggregatedKLine aggregatedKLine : aggregatedKLines) {
            if (save(aggregatedKLine)) {
                successCount.incrementAndGet();
            }
        }
        
        log.debug("批量保存聚合K线: 总数={}, 成功={}", aggregatedKLines.size(), successCount.get());
        return successCount.get();
    }
    
    @Override
    public boolean exists(String symbol, String period, long timestamp) {
        try {
            // 时间戳语义：epoch millis (UTC)，转换为Instant后查询QuestDB TIMESTAMP (UTC)
            Instant timestampInstant = Instant.ofEpochMilli(timestamp);
            Timestamp ts = Timestamp.from(timestampInstant);
            
            String existsSql = getExistsSql(period);
            
            Integer count = questDbJdbcTemplate.queryForObject(
                    existsSql,
                    Integer.class,
                    symbol,
                    ts
            );
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("检查聚合K线是否存在失败: symbol={}, period={}, timestamp={}", 
                    symbol, period, timestamp, e);
            return false;
        }
    }
    
    /**
     * 获取写入统计信息
     */
    public WriteStats getWriteStats() {
        return new WriteStats(
                totalWriteCount.get(),
                successWriteCount.get(),
                skipWriteCount.get(),
                failWriteCount.get()
        );
    }
    
    /**
     * 写入统计信息
     */
    public record WriteStats(
            long totalWriteCount,
            long successWriteCount,
            long skipWriteCount,
            long failWriteCount
    ) {
        /**
         * 计算写入成功率
         */
        public double getSuccessRate() {
            if (totalWriteCount == 0) {
                return 0.0;
            }
            return (double) successWriteCount / totalWriteCount * 100.0;
        }
    }
}

