package com.qyl.v2trade.market.aggregation.scheduler;

import com.qyl.v2trade.market.aggregation.core.KlineAggregator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 聚合清理定时任务调度器
 * 
 * <p>负责定期清理过期的Bucket，防止内存泄漏
 *
 * @author qyl
 */
@Slf4j
@Component
public class AggregationCleanupScheduler {
    
    @Autowired
    private KlineAggregator klineAggregator;
    
    /**
     * 清理过期Bucket定时任务
     * 每30分钟执行一次
     */
    @Scheduled(fixedRate = 1800000) // 1800000ms = 30分钟
    public void cleanupExpiredBuckets() {
        log.debug("开始执行过期Bucket清理任务");
        
        try {
            // 获取清理前的统计信息
            var statsBefore = klineAggregator.getStats();
            int activeBucketsBefore = statsBefore.activeBucketCount();
            
            // 执行清理
            klineAggregator.cleanupExpiredBuckets();
            
            // 获取清理后的统计信息
            var statsAfter = klineAggregator.getStats();
            int activeBucketsAfter = statsAfter.activeBucketCount();
            int cleanedCount = activeBucketsBefore - activeBucketsAfter;
            
            if (cleanedCount > 0) {
                log.info("过期Bucket清理完成: 清理前={}, 清理后={}, 清理数量={}", 
                        activeBucketsBefore, activeBucketsAfter, cleanedCount);
            } else {
                log.debug("过期Bucket清理完成: 无过期Bucket需要清理, 当前活跃Bucket数量={}", 
                        activeBucketsAfter);
            }
            
        } catch (Exception e) {
            log.error("过期Bucket清理任务执行失败", e);
        }
    }
}

