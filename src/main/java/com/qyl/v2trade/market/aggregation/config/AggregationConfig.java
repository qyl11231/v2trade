package com.qyl.v2trade.market.aggregation.config;

import com.qyl.v2trade.market.aggregation.core.KlineAggregator;
import com.qyl.v2trade.market.aggregation.core.impl.KlineAggregatorImpl;
import com.qyl.v2trade.market.aggregation.event.AggregationEventPublisher;
import com.qyl.v2trade.market.aggregation.persistence.AggregatedKLineStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 聚合模块配置类
 * 
 * <p>配置KlineAggregator和相关Bean
 *
 * @author qyl
 */
@Configuration
public class AggregationConfig {
    
    @Autowired
    private AggregationEventPublisher aggregationEventPublisher;
    
    @Autowired(required = false)
    private AggregatedKLineStorageService aggregatedKLineStorageService;
    
    /**
     * 创建KlineAggregator Bean
     * 
     * @return KlineAggregator实例
     */
    @Bean
    public KlineAggregator klineAggregator() {
        KlineAggregatorImpl aggregator = new KlineAggregatorImpl();
        
        // 设置聚合完成回调（发布事件）
        aggregator.setAggregationCallback(aggregatedKLine -> {
            aggregationEventPublisher.publish(aggregatedKLine);
        });
        
        // 设置存储服务（如果存在）
        if (aggregatedKLineStorageService != null) {
            aggregator.setStorageService(aggregatedKLineStorageService);
        }
        
        return aggregator;
    }
}

