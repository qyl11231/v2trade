package com.qyl.v2trade.market.subscription.collector.eventbus.impl;

import com.qyl.v2trade.market.subscription.collector.eventbus.MarketEventBus;
import com.qyl.v2trade.market.model.event.KlineEvent;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 简单行情事件总线实现（v1.0 简化版）
 * 
 * <p>使用线程池异步执行消费者，防止阻塞 WebSocket IO 线程。
 * 
 * <p>关键设计：
 * <ul>
 *   <li>使用 CopyOnWriteArrayList 存储订阅者，保证线程安全</li>
 *   <li>使用 ExecutorService 异步执行消费者，防止阻塞 WebSocket IO 线程</li>
 *   <li>单个订阅者异常不影响其他订阅者</li>
 * </ul>
 *
 * @author qyl
 */
@Slf4j
public class SimpleMarketEventBus implements MarketEventBus {

    /**
     * 订阅者列表（线程安全）
     */
    private final CopyOnWriteArrayList<Consumer<KlineEvent>> subscribers = new CopyOnWriteArrayList<>();

    /**
     * 线程池（用于异步执行消费者）
     * 
     * <p>使用固定大小线程池，避免创建过多线程。
     * 如果消费者处理较慢，可以考虑增加线程数或使用 CachedThreadPool。
     */
    private final ExecutorService executor = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "MarketEventBus-Worker");
        t.setDaemon(true);
        return t;
    });

    @Override
    public void publish(KlineEvent event) {
        if (event == null) {
            log.warn("发布的事件为 null，跳过");
            return;
        }

        if (subscribers.isEmpty()) {
            log.debug("没有订阅者，事件被丢弃: symbol={}, timestamp={}", 
                    event.symbol(), event.openTime());
            return;
        }

        // 异步执行所有订阅者，防止阻塞 WebSocket IO 线程
        for (Consumer<KlineEvent> consumer : subscribers) {
            executor.submit(() -> {
                try {
                    consumer.accept(event);
                } catch (Exception e) {
                    log.error("EventBus 消费者异常: symbol={}, timestamp={}", 
                            event.symbol(), event.openTime(), e);
                    // 异常隔离：单个订阅者异常不影响其他订阅者
                }
            });
        }

        log.debug("事件已发布到 {} 个订阅者: symbol={}, timestamp={}", 
                subscribers.size(), event.symbol(), event.openTime());
    }

    @Override
    public void subscribe(Consumer<KlineEvent> consumer) {
        if (consumer == null) {
            log.warn("订阅者为 null，跳过");
            return;
        }

        subscribers.add(consumer);
        log.info("新增事件订阅者，当前订阅者数量: {}", subscribers.size());
    }

    @Override
    public void unsubscribe(Consumer<KlineEvent> consumer) {
        if (consumer == null) {
            log.warn("取消订阅者为 null，跳过");
            return;
        }

        boolean removed = subscribers.remove(consumer);
        if (removed) {
            log.info("移除事件订阅者，当前订阅者数量: {}", subscribers.size());
        } else {
            log.warn("未找到要移除的订阅者");
        }
    }

    /**
     * 优雅关闭
     * 
     * <p>关闭线程池，等待正在执行的任务完成。
     */
    @PreDestroy
    public void shutdown() {
        log.info("开始关闭 MarketEventBus...");
        
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("MarketEventBus 线程池未在 5 秒内关闭，强制关闭");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("等待 MarketEventBus 线程池关闭时被中断", e);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        subscribers.clear();
        log.info("MarketEventBus 已关闭");
    }
}

