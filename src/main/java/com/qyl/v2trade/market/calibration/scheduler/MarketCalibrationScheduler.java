package com.qyl.v2trade.market.calibration.scheduler;

import com.qyl.v2trade.market.calibration.common.ExecutionMode;
import com.qyl.v2trade.market.calibration.common.TaskType;
import com.qyl.v2trade.market.calibration.config.entity.MarketCalibrationTaskConfig;
import com.qyl.v2trade.market.calibration.config.service.MarketCalibrationTaskConfigService;
import com.qyl.v2trade.market.calibration.executor.dto.TaskExecutionResult;
import com.qyl.v2trade.market.calibration.log.service.MarketCalibrationTaskLogService;
import com.qyl.v2trade.market.calibration.service.MarketCalibrationExecutionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * 行情校准定时任务调度器
 * 
 * <p>负责自动执行启用的自动任务配置：
 * <ul>
 *   <li>数据核对任务（DATA_VERIFY）</li>
 *   <li>缺失数据检测任务（MISSING_DATA）</li>
 * </ul>
 * 
 * <p>执行逻辑：
 * <ul>
 *   <li>定时扫描所有 executionMode = AUTO 且 enabled = 1 的任务配置</li>
 *   <li>根据 intervalHours 计算时间范围（当前时间 - intervalHours 到 当前时间）</li>
 *   <li>检查是否有正在执行的相同任务，避免重复执行</li>
 *   <li>调用执行服务执行任务</li>
 * </ul>
 * 
 * <p>重构：使用 Instant 作为时间参数，遵循 UTC Everywhere 原则
 *
 * @author qyl
 */
@Slf4j
@Component
public class MarketCalibrationScheduler {

    @Autowired
    private MarketCalibrationTaskConfigService taskConfigService;

    @Autowired
    private MarketCalibrationExecutionService executionService;

    @Autowired
    private MarketCalibrationTaskLogService taskLogService;

    /**
     * 数据核对定时任务
     * 每1小时执行一次
     */
    //@Scheduled(fixedRate = 3600000) // 3600000ms = 1小时
    public void scheduleDataVerify() {
        log.info("开始执行数据核对定时任务");
        
        try {
            // 1. 查询所有启用的自动数据核对任务配置
            List<MarketCalibrationTaskConfig> configs = taskConfigService.listEnabledAutoTasks(TaskType.DATA_VERIFY);
            
            if (configs.isEmpty()) {
                log.debug("没有启用的自动数据核对任务配置");
                return;
            }
            
            log.info("找到 {} 个启用的自动数据核对任务配置", configs.size());
            
            // 2. 遍历每个配置并执行
            for (MarketCalibrationTaskConfig config : configs) {
                executeTaskConfig(config);
            }
            
            log.info("数据核对定时任务执行完成");
        } catch (Exception e) {
            log.error("数据核对定时任务执行失败", e);
        }
    }

    /**
     * 缺失数据检测定时任务
     * 每2小时执行一次
     */
 //   @Scheduled(fixedRate = 7200000) // 7200000ms = 2小时
    public void scheduleMissingData() {
        log.info("开始执行缺失数据检测定时任务");
        
        try {
            // 1. 查询所有启用的自动缺失数据检测任务配置
            List<MarketCalibrationTaskConfig> configs = taskConfigService.listEnabledAutoTasks(TaskType.MISSING_DATA);
            
            if (configs.isEmpty()) {
                log.debug("没有启用的自动缺失数据检测任务配置");
                return;
            }
            
            log.info("找到 {} 个启用的自动缺失数据检测任务配置", configs.size());
            
            // 2. 遍历每个配置并执行
            for (MarketCalibrationTaskConfig config : configs) {
                executeTaskConfig(config);
            }
            
            log.info("缺失数据检测定时任务执行完成");
        } catch (Exception e) {
            log.error("缺失数据检测定时任务执行失败", e);
        }
    }

    /**
     * 执行单个任务配置
     * 
     * @param config 任务配置
     */
    private void executeTaskConfig(MarketCalibrationTaskConfig config) {
        Long taskConfigId = config.getId();
        String taskName = config.getTaskName();
        
        try {
            log.info("准备执行任务: taskConfigId={}, taskName={}, taskType={}", 
                    taskConfigId, taskName, config.getTaskType());
            
            // 1. 检查是否有正在执行的相同任务（避免重复执行）
            boolean isRunning = taskLogService.existsRunningLog(taskConfigId);
            if (isRunning) {
                log.warn("任务正在执行中，跳过本次调度: taskConfigId={}, taskName={}", 
                        taskConfigId, taskName);
                return;
            }
            
            // 2. 验证配置的有效性
            if (!ExecutionMode.AUTO.equals(config.getExecutionMode())) {
                log.warn("任务配置不是自动模式，跳过: taskConfigId={}, executionMode={}", 
                        taskConfigId, config.getExecutionMode());
                return;
            }
            
            if (config.getIntervalHours() == null || config.getIntervalHours() <= 0) {
                log.warn("任务配置的检测周期无效，跳过: taskConfigId={}, intervalHours={}", 
                        taskConfigId, config.getIntervalHours());
                return;
            }
            
            // 3. 计算时间范围（当前时间 - intervalHours 到 当前时间）
            // 重构：使用 Instant.now() 获取 UTC 时间，遵循 UTC Everywhere 原则
            Instant endTime = Instant.now();
            Instant startTime = endTime.minusSeconds(config.getIntervalHours() * 3600L);
            
            log.info("执行任务: taskConfigId={}, taskName={}, startTime={}, endTime={}, intervalHours={}", 
                    taskConfigId, taskName, startTime, endTime, config.getIntervalHours());
            
            // 4. 调用执行服务执行任务
            TaskExecutionResult result = executionService.executeTask(taskConfigId, startTime, endTime);
            
            if (result.isSuccess()) {
                log.info("任务执行成功: taskConfigId={}, taskName={}, duration={}ms", 
                        taskConfigId, taskName, result.getExecuteDurationMs());
            } else {
                log.error("任务执行失败: taskConfigId={}, taskName={}, error={}", 
                        taskConfigId, taskName, result.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("执行任务配置失败: taskConfigId={}, taskName={}", 
                    taskConfigId, taskName, e);
            // 不抛出异常，避免影响其他任务的执行
        }
    }
}

