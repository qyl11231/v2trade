package com.qyl.v2trade.indicator.domain.event.impl;

import com.qyl.v2trade.indicator.domain.event.BarClosedEvent;
import com.qyl.v2trade.indicator.domain.event.BarClosedEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 简单的BarClosedEvent发布器实现
 * 
 * <p>使用内存队列，同步发布
 *
 * @author qyl
 */
@Slf4j
@Component
public class SimpleBarClosedEventPublisher implements BarClosedEventPublisher {
    
    private final CopyOnWriteArrayList<Consumer<BarClosedEvent>> subscribers = new CopyOnWriteArrayList<>();
    
    @Override
    public void publish(BarClosedEvent event) {
        if (event == null) {
            log.warn("发布BarClosedEvent失败: event为null");
            return;
        }
        
         log.debug("发布BarClosedEvent: symbol={}, timeframe={}, barCloseTime={}",
                event.symbol(), event.timeframe(), event.barCloseTime());
        
        // 同步发布给所有订阅者
        for (Consumer<BarClosedEvent> consumer : subscribers) {
            try {
                consumer.accept(event);
            } catch (Exception e) {
                log.error("BarClosedEvent订阅者处理异常: symbol={}, timeframe={}",
                        event.symbol(), event.timeframe(), e);
            }
        }
    }
    
    @Override
    public void subscribe(Consumer<BarClosedEvent> consumer) {
        if (consumer == null) {
            throw new IllegalArgumentException("订阅者不能为null");
        }
        subscribers.add(consumer);
        log.debug("BarClosedEvent新增订阅者，当前订阅者数量: {}", subscribers.size());
    }
    
    @Override
    public void unsubscribe(Consumer<BarClosedEvent> consumer) {
        if (consumer == null) {
            return;
        }
        subscribers.remove(consumer);
        log.debug("BarClosedEvent取消订阅，当前订阅者数量: {}", subscribers.size());
    }
    
    /**
     * 获取当前订阅者数量
     */
    public int getSubscriberCount() {
        return subscribers.size();
    }
}

