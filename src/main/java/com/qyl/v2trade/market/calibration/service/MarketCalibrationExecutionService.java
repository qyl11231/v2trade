package com.qyl.v2trade.market.calibration.service;

import com.qyl.v2trade.market.calibration.executor.dto.TaskExecutionResult;

import java.time.Instant;

/**
 * 行情校准任务执行服务接口
 * 
 * <p>重构：使用 Instant 作为时间参数，遵循 UTC Everywhere 原则
 */
public interface MarketCalibrationExecutionService {

    /**
     * 执行任务
     * 
     * @param taskConfigId 任务配置ID
     * @param startTime 开始时间（UTC Instant）
     * @param endTime 结束时间（UTC Instant）
     * @return 执行结果
     */
    TaskExecutionResult executeTask(Long taskConfigId, Instant startTime, Instant endTime);
}

