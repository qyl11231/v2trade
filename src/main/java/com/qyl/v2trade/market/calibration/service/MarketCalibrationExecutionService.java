package com.qyl.v2trade.market.calibration.service;

import com.qyl.v2trade.market.calibration.executor.dto.TaskExecutionResult;

import java.time.LocalDateTime;

/**
 * 行情校准任务执行服务接口
 */
public interface MarketCalibrationExecutionService {

    /**
     * 执行任务
     * 
     * @param taskConfigId 任务配置ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 执行结果
     */
    TaskExecutionResult executeTask(Long taskConfigId, LocalDateTime startTime, LocalDateTime endTime);
}

