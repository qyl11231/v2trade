package com.qyl.v2trade.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务配置
 * 为WebSocket重连等异步任务提供Spring管理的线程池
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * WebSocket重连线程池
     * 使用Spring管理的线程池，确保线程有Spring上下文，可以正确获取数据库连接
     */
    @Bean(name = "websocketReconnectExecutor")
    public Executor websocketReconnectExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("WebSocket-Reconnect-");
        executor.setWaitForTasksToCompleteOnShutdown(true); // 关闭时等待任务完成
        executor.setAwaitTerminationSeconds(5); // 最多等待5秒
        executor.initialize();
        return executor;
    }
}

