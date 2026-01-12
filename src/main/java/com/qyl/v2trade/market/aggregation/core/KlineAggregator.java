package com.qyl.v2trade.market.aggregation.core;

import com.qyl.v2trade.market.aggregation.config.SupportedPeriod;
import com.qyl.v2trade.market.model.event.KlineEvent;

import java.util.List;

/**
 * K线聚合器接口
 * 
 * <p>负责将1m K线事件聚合成多周期K线
 *
 * @author qyl
 */
public interface KlineAggregator {
    
    /**
     * 处理1m K线事件
     * 
     * @param event 1m K线事件
     */
    void onKlineEvent(KlineEvent event);
    
    /**
     * 获取聚合统计信息
     * 
     * @return 统计信息
     */
    AggregationStats getStats();
    
    /**
     * 清理过期Bucket（定时任务调用）
     */
    void cleanupExpiredBuckets();
    

    

}

