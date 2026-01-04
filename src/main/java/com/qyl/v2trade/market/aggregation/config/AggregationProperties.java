package com.qyl.v2trade.market.aggregation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 聚合模块配置属性
 * 
 * <p>用于配置聚合模块的行为
 *
 * @author qyl
 */
@Data
@Component
@ConfigurationProperties(prefix = "market.aggregation")
public class AggregationProperties {
    
    /**
     * 启动时是否补齐历史数据
     * 默认：true
     */
    private boolean enableHistoryBackfill = true;
    
    /**
     * 历史数据扫描范围（小时）
     * 默认：24小时
     */
    private int historyScanHours = 24;
    
    /**
     * 初始化是否异步执行
     * 默认：true（不阻塞启动）
     */
    private boolean asyncInitialization = true;
}

