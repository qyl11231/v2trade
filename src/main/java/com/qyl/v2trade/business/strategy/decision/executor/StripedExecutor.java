package com.qyl.v2trade.business.strategy.decision.executor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 分片执行器（StripedExecutor）
 * 
 * <p>职责：
 * <ul>
 *   <li>保证同一StrategyInstance的决策串行执行</li>
 *   <li>基于strategyId + tradingPairId做hash分片</li>
 *   <li>每个分片使用单线程队列</li>
 * </ul>
 * 
 * <p>设计原理：
 * <ul>
 *   <li>使用hash分片，将不同的实例分配到不同的线程</li>
 *   <li>同一实例的所有任务都在同一个线程中串行执行</li>
 *   <li>不同实例可以并行执行（提高吞吐量）</li>
 * </ul>
 * 
 * <p>阶段2约束：
 * <ul>
 *   <li>同一StrategyInstance必须串行执行</li>
 *   <li>避免并发决策导致的状态不一致</li>
 * </ul>
 */
@Slf4j
@Component
public class StripedExecutor {

    /**
     * 线程池大小（默认CPU核心数）
     */
    @Value("${strategy.decision.executor.core-pool-size:4}")
    private int corePoolSize;

    @Value("${strategy.decision.executor.max-pool-size:8}")
    private int maxPoolSize;

    @Value("${strategy.decision.executor.queue-capacity:1000}")
    private int queueCapacity;

    /**
     * 分片数组（每个分片是一个单线程执行器）
     */
    private ExecutorService[] stripes;

    /**
     * 分片数量（等于corePoolSize）
     */
    private int stripeCount;

    /**
     * 线程计数器（用于命名）
     */
    private final AtomicInteger threadCounter = new AtomicInteger(0);

    /**
     * 初始化执行器
     */
    @PostConstruct
    public void init() {
        // 确保分片数量至少为1
        if (corePoolSize <= 0) {
            corePoolSize = Runtime.getRuntime().availableProcessors();
            log.warn("corePoolSize配置无效，使用默认值: {}", corePoolSize);
        }

        stripeCount = corePoolSize;
        stripes = new ExecutorService[stripeCount];

        // 创建每个分片的单线程执行器
        for (int i = 0; i < stripeCount; i++) {
            final int stripeIndex = i;
            stripes[i] = new ThreadPoolExecutor(
                    1, 1,  // 单线程
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(queueCapacity),
                    r -> {
                        Thread t = new Thread(r, "decision-executor-stripe-" + stripeIndex + "-" + threadCounter.incrementAndGet());
                        t.setDaemon(false);
                        return t;
                    },
                    new ThreadPoolExecutor.CallerRunsPolicy()  // 队列满时由调用线程执行
            );
        }

        log.info("StripedExecutor初始化完成: stripeCount={}, queueCapacity={}", stripeCount, queueCapacity);
    }

    /**
     * 销毁执行器
     */
    @PreDestroy
    public void destroy() {
        if (stripes != null) {
            for (ExecutorService stripe : stripes) {
                stripe.shutdown();
                try {
                    if (!stripe.awaitTermination(10, TimeUnit.SECONDS)) {
                        stripe.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    stripe.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
            log.info("StripedExecutor已关闭");
        }
    }

    /**
     * 提交任务到对应的分片执行器
     * 
     * <p>根据instanceKey的hash值选择分片，保证同一实例的任务都在同一分片执行
     * 
     * @param instanceKey 实例键
     * @param task 任务
     * @return Future对象
     */
    public Future<?> submit(InstanceKey instanceKey, Runnable task) {
        if (instanceKey == null) {
            throw new IllegalArgumentException("实例键不能为null");
        }
        if (task == null) {
            throw new IllegalArgumentException("任务不能为null");
        }

        // 计算分片索引（使用hash）
        int stripeIndex = getStripeIndex(instanceKey);
        ExecutorService stripe = stripes[stripeIndex];

        log.debug("提交决策任务到分片: instanceKey={}, stripeIndex={}", instanceKey, stripeIndex);

        return stripe.submit(() -> {
            try {
                task.run();
            } catch (Exception e) {
                log.error("决策任务执行失败: instanceKey={}, stripeIndex={}", instanceKey, stripeIndex, e);
                throw e;
            }
        });
    }

    /**
     * 同步执行任务（阻塞直到完成）
     * 
     * @param instanceKey 实例键
     * @param task 任务
     * @throws ExecutionException 如果任务执行失败
     * @throws InterruptedException 如果被中断
     */
    public void execute(InstanceKey instanceKey, Runnable task) throws ExecutionException, InterruptedException {
        Future<?> future = submit(instanceKey, task);
        future.get();  // 等待完成
    }

    /**
     * 计算分片索引
     * 
     * <p>使用instanceKey的hash值对分片数量取模
     * 
     * @param instanceKey 实例键
     * @return 分片索引（0到stripeCount-1）
     */
    private int getStripeIndex(InstanceKey instanceKey) {
        // 使用strategyId和tradingPairId的组合hash
        int hash = (instanceKey.getStrategyId().hashCode() * 31) + instanceKey.getTradingPairId().hashCode();
        // 取绝对值并取模
        return Math.abs(hash) % stripeCount;
    }

    /**
     * 获取分片数量
     * 
     * @return 分片数量
     */
    public int getStripeCount() {
        return stripeCount;
    }

    /**
     * 获取指定分片的队列大小
     * 
     * @param stripeIndex 分片索引
     * @return 队列大小
     */
    public int getQueueSize(int stripeIndex) {
        if (stripeIndex < 0 || stripeIndex >= stripeCount) {
            return -1;
        }
        ExecutorService stripe = stripes[stripeIndex];
        if (stripe instanceof ThreadPoolExecutor) {
            return ((ThreadPoolExecutor) stripe).getQueue().size();
        }
        return -1;
    }
}

