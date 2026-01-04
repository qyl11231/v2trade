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
    
    /**
     * 初始化历史数据补齐（启动时调用）
     * 
     * <p>扫描并补齐启动前未完成的聚合窗口
     * 
     * @param symbols 需要补齐的交易对列表（如果为空，则补齐所有交易对）
     */
    void initializeHistoryBackfill(List<String> symbols);
    
    /**
     * 补齐指定窗口的历史数据
     * 
     * @param symbol 交易对
     * @param period 周期
     * @param windowStart 窗口起始时间戳
     */
    void backfillWindow(String symbol, SupportedPeriod period, long windowStart);
}

