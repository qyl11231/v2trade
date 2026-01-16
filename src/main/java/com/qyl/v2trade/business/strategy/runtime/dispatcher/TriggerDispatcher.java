package com.qyl.v2trade.business.strategy.runtime.dispatcher;

import com.qyl.v2trade.business.strategy.runtime.dedup.TriggerDeduplicator;
import com.qyl.v2trade.business.strategy.runtime.logger.TriggerLogger;
import com.qyl.v2trade.business.strategy.runtime.manager.StrategyRuntimeManager;
import com.qyl.v2trade.business.strategy.runtime.router.EventRouter;
import com.qyl.v2trade.business.strategy.runtime.trigger.StrategyTrigger;
import com.qyl.v2trade.business.strategy.runtime.trigger.TriggerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 异步事件分发器
 * 
 * <p>负责异步消费事件，不阻塞上游线程
 *
 * @author qyl
 */
@Component
public class TriggerDispatcher {
    
    private static final Logger logger = LoggerFactory.getLogger(TriggerDispatcher.class);
    
    @Autowired
    private TriggerDeduplicator deduplicator;
    
    @Autowired
    private EventRouter router;
    
    @Autowired(required = false)
    private TriggerLogger triggerLogger;  // N2 阶段使用，N3 阶段可选
    
    @Autowired(required = false)
    private StrategyRuntimeManager runtimeManager;  // N3 阶段使用
    
    // 队列：BAR_CLOSE/SIGNAL 高优先级，PRICE 低优先级
    private final BlockingQueue<StrategyTrigger> highPriorityQueue = new LinkedBlockingQueue<>(10_000);
    private final BlockingQueue<StrategyTrigger> lowPriorityQueue = new LinkedBlockingQueue<>(50_000);
    
    // 线程池
    private ThreadPoolExecutor executor;
    
    // 线程计数器（必须在类字段最上方声明，避免初始化顺序问题）
    private final AtomicInteger workerCounter = new AtomicInteger(0);
    
    @PostConstruct
    public void init() {
        // 核心线程数 = CPU 核心数
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        // 最大线程数 = 核心数 * 2
        int maxPoolSize = corePoolSize * 2;
        
        // 线程工厂（单独抽取，避免示例代码被直接 copy 后踩坑）
        ThreadFactory threadFactory = r -> {
            Thread t = new Thread(r, "strategy-trigger-worker-" + workerCounter.getAndIncrement());
            t.setDaemon(false);
            return t;
        };
        
        executor = new ThreadPoolExecutor(
            corePoolSize,
            maxPoolSize,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            threadFactory,
            new ThreadPoolExecutor.CallerRunsPolicy()  // 队列满时由调用线程执行
        );
        
        // 启动消费线程
        startConsumer();
        
        logger.info("TriggerDispatcher 初始化完成: corePoolSize={}, maxPoolSize={}", 
            corePoolSize, maxPoolSize);
    }
    
    private void startConsumer() {
        // 高优先级队列消费者
        executor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    StrategyTrigger trigger = highPriorityQueue.take();
                    processTrigger(trigger);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("处理高优先级事件失败", e);
                }
            }
        });
        
        // 低优先级队列消费者
        executor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    StrategyTrigger trigger = lowPriorityQueue.take();
                    processTrigger(trigger);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("处理低优先级事件失败", e);
                }
            }
        });
    }
    
    /**
     * 接收事件（由 TriggerIngress 调用）
     */
    public void dispatch(StrategyTrigger trigger) {
        // 幂等去重
        if (!deduplicator.shouldProcess(trigger.getEventKey())) {
            logger.debug("事件已去重，跳过: eventKey={}", trigger.getEventKey());
            return;
        }
        
        // 根据类型选择队列
        BlockingQueue<StrategyTrigger> queue = 
            (trigger.getTriggerType() == TriggerType.PRICE) ? lowPriorityQueue : highPriorityQueue;
        
        // 尝试入队
        boolean offered = queue.offer(trigger);
        if (!offered) {
            // 队列满：低优先级事件丢弃并打 warn；高优先级事件尽量不丢
            if (trigger.getTriggerType() == TriggerType.PRICE) {
                logger.warn("低优先级队列满，丢弃事件: eventKey={}", trigger.getEventKey());
            } else {
                logger.error("高优先级队列满，事件可能丢失: eventKey={}", trigger.getEventKey());
                // 可以考虑降级到低优先级队列或同步处理
            }
        }
    }
    
    private void processTrigger(StrategyTrigger trigger) {
        try {
            // 路由到实例
            List<Long> instanceIds = router.route(trigger);
            
            if (instanceIds.isEmpty()) {
                logger.debug("事件未路由到任何实例: eventKey={}", trigger.getEventKey());
                return;
            }
            
            // 【N3 改动】：分发到 RuntimeManager（如果存在）
            if (runtimeManager != null) {
                // N3 阶段：分发到 Runtime，由 Runtime 内部打印 runtime_event 日志
                for (Long instanceId : instanceIds) {
                    runtimeManager.dispatch(instanceId, trigger);
                }
            } else {
                // N2 阶段：只打印日志（兼容模式）
                if (triggerLogger != null) {
                    for (Long instanceId : instanceIds) {
                        Long userId = router.getUserIdByInstanceId(instanceId);
                        if (userId == null) {
                            logger.warn("无法从路由缓存获取 userId，跳过日志: instanceId={}", instanceId);
                            continue;
                        }
                        triggerLogger.log(trigger, instanceId, userId);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("处理事件失败: eventKey={}", trigger.getEventKey(), e);
        }
    }
    
    @PreDestroy
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}

