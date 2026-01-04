package com.qyl.v2trade.market.calibration.executor;

import com.qyl.v2trade.market.calibration.config.entity.MarketCalibrationTaskConfig;
import com.qyl.v2trade.market.calibration.executor.dto.TaskExecutionResult;

import java.time.LocalDateTime;

/**
 * 行情校准任务执行器接口
 */
public interface MarketCalibrationExecutor {

    /**
     * 执行任务
     * 
     * @param taskConfig 任务配置
     * @param startTime 检测开始时间
     * @param endTime 检测结束时间
     * @return 执行结果
     */
    TaskExecutionResult execute(MarketCalibrationTaskConfig taskConfig, LocalDateTime startTime, LocalDateTime endTime);
}

