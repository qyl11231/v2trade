package com.qyl.v2trade.market.calibration.service.impl;

import com.qyl.v2trade.exception.BusinessException;
import com.qyl.v2trade.market.calibration.common.TaskType;
import com.qyl.v2trade.market.calibration.config.entity.MarketCalibrationTaskConfig;
import com.qyl.v2trade.market.calibration.config.service.MarketCalibrationTaskConfigService;
import com.qyl.v2trade.market.calibration.executor.MarketCalibrationExecutor;
import com.qyl.v2trade.market.calibration.executor.dto.TaskExecutionResult;
import com.qyl.v2trade.market.calibration.executor.impl.DataVerifyExecutor;
import com.qyl.v2trade.market.calibration.executor.impl.MissingDataExecutor;
import com.qyl.v2trade.market.calibration.service.MarketCalibrationExecutionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * 行情校准任务执行服务实现类
 * 
 * <p>重构：使用 Instant 作为时间参数，遵循 UTC Everywhere 原则
 */
@Slf4j
@Service
public class MarketCalibrationExecutionServiceImpl implements MarketCalibrationExecutionService {

    @Autowired
    private MarketCalibrationTaskConfigService taskConfigService;

    @Autowired
    private MissingDataExecutor missingDataExecutor;

    @Autowired
    private DataVerifyExecutor dataVerifyExecutor;

    @Override
    public TaskExecutionResult executeTask(Long taskConfigId, Instant startTime, Instant endTime) {
        log.info("开始执行任务: taskConfigId={}, startTime={}, endTime={}", taskConfigId, startTime, endTime);

        // 1. 根据任务配置ID查询配置
        MarketCalibrationTaskConfig taskConfig = taskConfigService.getById(taskConfigId);
        if (taskConfig == null) {
            throw new BusinessException("任务配置不存在: taskConfigId=" + taskConfigId);
        }

        // 2. 根据任务类型选择执行器
        MarketCalibrationExecutor executor = getExecutor(taskConfig.getTaskType());
        if (executor == null) {
            throw new BusinessException("不支持的任务类型: " + taskConfig.getTaskType());
        }

        // 3. 调用执行器执行任务
        TaskExecutionResult result = executor.execute(taskConfig, startTime, endTime);

        log.info("任务执行完成: taskConfigId={}, success={}", taskConfigId, result.isSuccess());
        return result;
    }

    /**
     * 根据任务类型获取执行器
     */
    private MarketCalibrationExecutor getExecutor(String taskType) {
        if (TaskType.MISSING_DATA.equals(taskType)) {
            return missingDataExecutor;
        } else if (TaskType.DATA_VERIFY.equals(taskType)) {
            return dataVerifyExecutor;
        } else {
            return null;
        }
    }
}

