package com.qyl.v2trade.market.aggregation.event.impl;

import com.qyl.v2trade.market.aggregation.event.AggregatedKLine;
import com.qyl.v2trade.market.aggregation.event.AggregationEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 聚合事件发布器实现
 * 
 * <p>使用简单的Consumer列表机制发布事件
 * 
 * <p>注意：当前实现使用Consumer列表机制。后续可以扩展MarketEventBus以支持多类型事件。
 *
 * @author qyl
 */
@Slf4j
@Component
public class AggregationEventPublisherImpl implements AggregationEventPublisher {
    
    /**
     * 订阅者列表（线程安全）
     */
    private final CopyOnWriteArrayList<Consumer<AggregatedKLine>> subscribers = new CopyOnWriteArrayList<>();
    
    @Override
    public void publish(AggregatedKLine aggregatedKLine) {
        if (aggregatedKLine == null) {
            log.warn("发布的聚合事件为 null，跳过");
            return;
        }
        
        if (subscribers.isEmpty()) {
            log.debug("没有订阅者，聚合事件被丢弃: symbol={}, period={}, timestamp={}", 
                    aggregatedKLine.symbol(), aggregatedKLine.period(), aggregatedKLine.timestamp());
            return;
        }
        
        // 同步执行所有订阅者（聚合事件处理速度通常很快，不需要异步）
        for (Consumer<AggregatedKLine> consumer : subscribers) {
            try {
                consumer.accept(aggregatedKLine);
            } catch (Exception e) {
                log.error("聚合事件消费者异常: symbol={}, period={}, timestamp={}", 
                        aggregatedKLine.symbol(), aggregatedKLine.period(), aggregatedKLine.timestamp(), e);
                // 异常隔离：单个订阅者异常不影响其他订阅者
            }
        }
        
        log.debug("聚合事件已发布到 {} 个订阅者: symbol={}, period={}, timestamp={}", 
                subscribers.size(), aggregatedKLine.symbol(), aggregatedKLine.period(), aggregatedKLine.timestamp());
    }
    
    /**
     * 订阅聚合事件
     * 
     * @param consumer 事件消费者
     */
    public void subscribe(Consumer<AggregatedKLine> consumer) {
        if (consumer == null) {
            log.warn("订阅者为 null，跳过");
            return;
        }
        
        subscribers.add(consumer);
        log.info("新增聚合事件订阅者，当前订阅者数量: {}", subscribers.size());
    }
    
    /**
     * 取消订阅聚合事件
     * 
     * @param consumer 事件消费者
     */
    public void unsubscribe(Consumer<AggregatedKLine> consumer) {
        if (consumer == null) {
            log.warn("取消订阅者为 null，跳过");
            return;
        }
        
        boolean removed = subscribers.remove(consumer);
        if (removed) {
            log.info("移除聚合事件订阅者，当前订阅者数量: {}", subscribers.size());
        } else {
            log.warn("未找到要移除的聚合事件订阅者");
        }
    }
}

