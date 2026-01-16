package com.qyl.v2trade.business.strategy.runtime.dispatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 分片串行执行器
 * 
 * <p>【核心保证】：同一 instanceId 永远落到同一个 stripe → 串行天然成立
 * 
 * <p>使用 instanceId 做 hash，映射到 N 个 stripe（N = CPU 核心数 * 2）
 * 每个 stripe 是单线程队列执行器
 *
 * @author qyl
 */
@Component
public class StripedSerialExecutor {
    
    private static final Logger log = LoggerFactory.getLogger(StripedSerialExecutor.class);
    
    // Stripe 数量 = CPU 核心数 * 2
    private final int stripeCount;
    
    // 每个 stripe 是单线程队列执行器
    private final ThreadPoolExecutor[] stripes;
    
    // Metrics（用于监控）
    private final AtomicLong[] stripeQueueSizes;
    private final AtomicLong[] stripeRejectedCounts;
    private final AtomicLong[] stripeProcessedCounts;
    
    public StripedSerialExecutor() {
        this.stripeCount = Runtime.getRuntime().availableProcessors() * 2;
        this.stripes = new ThreadPoolExecutor[stripeCount];
        this.stripeQueueSizes = new AtomicLong[stripeCount];
        this.stripeRejectedCounts = new AtomicLong[stripeCount];
        this.stripeProcessedCounts = new AtomicLong[stripeCount];
        
        // 初始化每个 stripe
        for (int i = 0; i < stripeCount; i++) {
            final int stripeId = i;
            stripeQueueSizes[i] = new AtomicLong(0);
            stripeRejectedCounts[i] = new AtomicLong(0);
            stripeProcessedCounts[i] = new AtomicLong(0);
            
            stripes[i] = new ThreadPoolExecutor(
                1, 1,  // 单线程
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(10_000),  // 队列上限
                r -> {
                    Thread t = new Thread(r, "strategy-runtime-stripe-" + stripeId);
                    t.setDaemon(false);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()  // 队列满时由调用线程执行
            );
        }
        
        log.info("StripedSerialExecutor 初始化完成: stripeCount={}", stripeCount);
    }
    
    /**
     * 执行任务（根据 instanceId 哈希到对应的 stripe）
     * 
     * 【核心保证】：同一 instanceId 永远落到同一个 stripe → 串行天然成立
     * 
     * @param instanceId 策略实例ID
     * @param task 任务
     */
    public void execute(Long instanceId, Runnable task) {
        if (instanceId == null || task == null) {
            return;
        }
        
        int stripeId = getStripeId(instanceId);
        
        try {
            stripes[stripeId].submit(() -> {
                try {
                    stripeQueueSizes[stripeId].incrementAndGet();
                    stripeProcessedCounts[stripeId].incrementAndGet();
                    task.run();
                } catch (Exception e) {
                    log.error("Stripe 执行任务失败: stripeId={}, instanceId={}", stripeId, instanceId, e);
                } finally {
                    stripeQueueSizes[stripeId].decrementAndGet();
                }
            });
        } catch (RejectedExecutionException e) {
            stripeRejectedCounts[stripeId].incrementAndGet();
            handleRejection(instanceId, task, stripeId, e);
        }
    }
    
    /**
     * 处理拒绝（队列满）
     * 
     * <p>N3 最小实现：PRICE 可丢弃，BAR_CLOSE/SIGNAL 尽量不丢
     * 
     * @param instanceId 实例ID
     * @param task 任务
     * @param stripeId Stripe ID
     * @param e 拒绝异常
     */
    private void handleRejection(Long instanceId, Runnable task, int stripeId, RejectedExecutionException e) {
        log.warn("Stripe 队列满，任务被拒绝: instanceId={}, stripeId={}, queueSize={}", 
            instanceId, stripeId, stripeQueueSizes[stripeId].get(), e);
        
        // TODO: N3 阶段可以根据事件类型决定是否丢弃
        // PRICE 可丢弃，BAR_CLOSE/SIGNAL 需要重试或降级处理
    }
    
    /**
     * 获取 Stripe ID（根据 instanceId 哈希）
     * 
     * @param instanceId 实例ID
     * @return Stripe ID (0 到 stripeCount-1)
     */
    private int getStripeId(Long instanceId) {
        return (int) (Math.abs(instanceId) % stripeCount);
    }
    
    /**
     * 获取 Stripe 数量
     * 
     * @return Stripe 数量
     */
    public int getStripeCount() {
        return stripeCount;
    }
    
    // Metrics 方法
    
    /**
     * 获取指定 Stripe 的队列长度
     * 
     * @param stripeId Stripe ID
     * @return 队列长度
     */
    public long getStripeQueueSize(int stripeId) {
        if (stripeId < 0 || stripeId >= stripeCount) {
            return 0;
        }
        return stripeQueueSizes[stripeId].get();
    }
    
    /**
     * 获取指定 Stripe 的拒绝次数
     * 
     * @param stripeId Stripe ID
     * @return 拒绝次数
     */
    public long getStripeRejectedCount(int stripeId) {
        if (stripeId < 0 || stripeId >= stripeCount) {
            return 0;
        }
        return stripeRejectedCounts[stripeId].get();
    }
    
    /**
     * 获取指定 Stripe 的处理次数
     * 
     * @param stripeId Stripe ID
     * @return 处理次数
     */
    public long getStripeProcessedCount(int stripeId) {
        if (stripeId < 0 || stripeId >= stripeCount) {
            return 0;
        }
        return stripeProcessedCounts[stripeId].get();
    }
    
    /**
     * 获取所有 Stripe 的总队列长度
     * 
     * @return 总队列长度
     */
    public long getTotalQueueSize() {
        long total = 0;
        for (int i = 0; i < stripeCount; i++) {
            total += stripeQueueSizes[i].get();
        }
        return total;
    }
    
    @PreDestroy
    public void shutdown() {
        log.info("StripedSerialExecutor 正在关闭...");
        for (int i = 0; i < stripeCount; i++) {
            if (stripes[i] != null) {
                stripes[i].shutdown();
                try {
                    if (!stripes[i].awaitTermination(10, TimeUnit.SECONDS)) {
                        stripes[i].shutdownNow();
                    }
                } catch (InterruptedException e) {
                    stripes[i].shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }
        log.info("StripedSerialExecutor 已关闭");
    }
}

