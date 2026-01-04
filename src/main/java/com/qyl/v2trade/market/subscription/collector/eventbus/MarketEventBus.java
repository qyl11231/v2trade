package com.qyl.v2trade.market.subscription.collector.eventbus;

import com.qyl.v2trade.market.model.event.KlineEvent;

import java.util.function.Consumer;

/**
 * 行情事件总线接口
 * 
 * <p>用于在行情订阅模块内部进行事件发布和订阅，实现解耦的事件驱动架构。
 * 
 * <p>所有行情事件（K线、深度、成交等）通过该接口进行发布和订阅。
 *
 * @author qyl
 */
public interface MarketEventBus {

    /**
     * 发布 K 线事件
     * 
     * @param event K 线事件
     */
    void publish(KlineEvent event);

    /**
     * 订阅 K 线事件
     * 
     * @param consumer 事件消费者
     */
    void subscribe(Consumer<KlineEvent> consumer);

    /**
     * 取消订阅 K 线事件
     * 
     * @param consumer 事件消费者
     */
    void unsubscribe(Consumer<KlineEvent> consumer);
}

