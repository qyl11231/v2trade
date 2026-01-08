package com.qyl.v2trade.market.aggregation.config;

import com.qyl.v2trade.indicator.domain.event.BarClosedEvent;
import com.qyl.v2trade.indicator.domain.event.BarClosedEventPublisher;
import com.qyl.v2trade.indicator.infrastructure.converter.AggregatedKLineToBarClosedEventConverter;
import com.qyl.v2trade.market.aggregation.core.KlineAggregator;
import com.qyl.v2trade.market.aggregation.core.impl.KlineAggregatorImpl;
import com.qyl.v2trade.market.aggregation.event.AggregationEventPublisher;
import com.qyl.v2trade.market.aggregation.persistence.AggregatedKLineStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
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
    
    @Autowired(required = false)
    private BarClosedEventPublisher barClosedEventPublisher;
    
    @Autowired(required = false)
    private AggregatedKLineToBarClosedEventConverter aggregatedKLineToBarClosedEventConverter;
    
    @Autowired(required = false)
    private ApplicationEventPublisher applicationEventPublisher;
    
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
            // 1. 发布原有的AggregationEvent
            aggregationEventPublisher.publish(aggregatedKLine);
            
            // 2. 发布BarClosedEvent（指标模块使用）
            // 优先使用Spring ApplicationEventPublisher（支持@EventListener）
            if (aggregatedKLineToBarClosedEventConverter != null) {
                try {
                    var barClosedEvent = aggregatedKLineToBarClosedEventConverter.convert(aggregatedKLine);
                    
                    // 方案1：使用Spring ApplicationEventPublisher（@EventListener会自动接收）
                    if (applicationEventPublisher != null) {
                        applicationEventPublisher.publishEvent(barClosedEvent);
                    }
                    
                    // 方案2：同时使用自定义Publisher（向后兼容）
                    if (barClosedEventPublisher != null) {
                        barClosedEventPublisher.publish(barClosedEvent);
                    }
                } catch (Exception e) {
                    // 日志记录但不影响原有流程
                    org.slf4j.LoggerFactory.getLogger(AggregationConfig.class)
                        .error("发布BarClosedEvent失败: symbol={}, period={}", 
                            aggregatedKLine.symbol(), aggregatedKLine.period(), e);
                }
            }
        });
        
        // 设置存储服务（如果存在）
        if (aggregatedKLineStorageService != null) {
            aggregator.setStorageService(aggregatedKLineStorageService);
        }
        
        return aggregator;
    }
}

