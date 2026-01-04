package com.qyl.v2trade.market.aggregation.core;

import java.io.Serializable;

/**
 * 聚合统计信息
 * 
 * <p>用于监控聚合器的运行状态
 *
 * @author qyl
 */
public record AggregationStats(
    /**
     * 活跃Bucket数量
     */
    int activeBucketCount,
    
    /**
     * 总处理的K线事件数量
     */
    long totalEventCount,
    
    /**
     * 总生成的聚合K线数量
     */
    long totalAggregatedCount
) implements Serializable {
    
    /**
     * 创建统计信息
     */
    public static AggregationStats of(int activeBucketCount, long totalEventCount, long totalAggregatedCount) {
        return new AggregationStats(activeBucketCount, totalEventCount, totalAggregatedCount);
    }
    
    /**
     * 创建空的统计信息
     */
    public static AggregationStats empty() {
        return new AggregationStats(0, 0, 0);
    }
}

