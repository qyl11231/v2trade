package com.qyl.v2trade.market.subscription.collector.eventbus;

import com.qyl.v2trade.market.model.event.PriceTick;

import java.util.function.Consumer;

/**
 * 价格事件总线接口（独立）
 * 
 * <p>用于在订阅模块内部进行价格事件发布和订阅，实现解耦的事件驱动架构。
 * 独立于MarketEventBus，专门处理价格事件，保持模块独立性。
 *
 * @author qyl
 */
public interface PriceEventBus {

    /**
     * 发布价格Tick事件
     * 
     * @param tick 价格Tick事件
     */
    void publish(PriceTick tick);

    /**
     * 订阅价格Tick事件
     * 
     * @param consumer 事件消费者
     */
    void subscribe(Consumer<PriceTick> consumer);

    /**
     * 取消订阅价格Tick事件
     * 
     * @param consumer 事件消费者
     */
    void unsubscribe(Consumer<PriceTick> consumer);
}

