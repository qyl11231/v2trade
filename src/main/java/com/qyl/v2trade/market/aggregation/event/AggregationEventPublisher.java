package com.qyl.v2trade.market.aggregation.event;

/**
 * 聚合事件发布器接口
 * 
 * <p>负责发布聚合完成的K线事件
 *
 * @author qyl
 */
public interface AggregationEventPublisher {
    
    /**
     * 发布聚合完成事件
     * 
     * @param aggregatedKLine 聚合K线事件
     */
    void publish(AggregatedKLine aggregatedKLine);
}

