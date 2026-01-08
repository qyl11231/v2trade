package com.qyl.v2trade.business.strategy.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * 策略启动监听器
 * 
 * <p>职责：
 * <ul>
 *   <li>监听Spring容器启动完成事件</li>
 *   <li>系统启动时自动加载所有启用策略</li>
 *   <li>异常不影响系统启动</li>
 * </ul>
 * 
 * <p>阶段1约束：
 * <ul>
 *   <li>只读操作，不修改数据库</li>
 *   <li>异常处理完善，不影响系统启动</li>
 * </ul>
 * 
 * @author 开发团队
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StrategyBootstrapListener implements ApplicationListener<ContextRefreshedEvent> {

    private final StrategyBootstrapper bootstrapper;

    /**
     * 处理容器刷新事件
     * 
     * <p>当Spring容器启动完成时，自动加载所有启用策略
     * 
     * @param event 容器刷新事件
     */
    @Override
    public void onApplicationEvent(@NonNull ContextRefreshedEvent event) {
        // 避免重复执行（Spring可能会触发多次）
        if (event.getApplicationContext().getParent() != null) {
            return;
        }

        log.info("Spring容器启动完成，开始加载所有启用策略");

        try {
            bootstrapper.bootstrapAll();
            log.info("策略加载完成");
        } catch (Exception e) {
            log.error("策略加载失败，但不影响系统启动", e);
            // 不抛出异常，允许系统继续启动
        }
    }
}

