package com.qyl.v2trade.indicator.domain.event;

import java.util.function.Consumer;

/**
 * BarClosedEvent发布器接口
 * 
 * <p>用于发布BarClosedEvent事件
 *
 * @author qyl
 */
public interface BarClosedEventPublisher {
    
    /**
     * 发布BarClosedEvent
     * 
     * @param event BarClosedEvent
     */
    void publish(BarClosedEvent event);
    
    /**
     * 订阅BarClosedEvent
     * 
     * @param consumer 消费者
     */
    void subscribe(Consumer<BarClosedEvent> consumer);
    
    /**
     * 取消订阅
     * 
     * @param consumer 消费者
     */
    void unsubscribe(Consumer<BarClosedEvent> consumer);
}

