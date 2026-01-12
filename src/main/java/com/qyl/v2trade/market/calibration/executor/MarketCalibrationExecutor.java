package com.qyl.v2trade.market.calibration.executor;

import com.qyl.v2trade.market.calibration.config.entity.MarketCalibrationTaskConfig;
import com.qyl.v2trade.market.calibration.executor.dto.TaskExecutionResult;

import java.time.Instant;

/**
 * 行情校准任务执行器接口
 * 
 * <p>重构：使用 Instant 作为时间参数，遵循 UTC Everywhere 原则
 */
public interface MarketCalibrationExecutor {

    /**
     * 执行任务
     * 
     * @param taskConfig 任务配置
     * @param startTime 检测开始时间（UTC Instant）
     * @param endTime 检测结束时间（UTC Instant）
     * @return 执行结果
     */
    TaskExecutionResult execute(MarketCalibrationTaskConfig taskConfig, Instant startTime, Instant endTime);
}

