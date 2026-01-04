package com.qyl.v2trade.market.calibration.executor.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qyl.v2trade.market.calibration.common.TaskLogStatus;
import com.qyl.v2trade.market.calibration.executor.MarketCalibrationExecutor;
import com.qyl.v2trade.market.calibration.executor.dto.TaskExecutionResult;
import com.qyl.v2trade.market.calibration.config.entity.MarketCalibrationTaskConfig;
import com.qyl.v2trade.market.calibration.log.dto.TaskLogCreateRequest;
import com.qyl.v2trade.market.calibration.log.dto.TaskLogUpdateRequest;
import com.qyl.v2trade.market.calibration.log.entity.MarketCalibrationTaskLog;
import com.qyl.v2trade.market.calibration.log.service.MarketCalibrationTaskLogService;
import com.qyl.v2trade.market.calibration.service.*;
import com.qyl.v2trade.market.calibration.service.dto.TradingPairInfo;
import com.qyl.v2trade.market.model.NormalizedKline;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 缺失数据检测执行器
 */
@Slf4j
@Component
public class MissingDataExecutor implements MarketCalibrationExecutor {

    @Autowired
    private TradingPairInfoService tradingPairInfoService;

    @Autowired
    private KlineGapDetector klineGapDetector;

    @Autowired
    private HistoricalKlineFetcher historicalKlineFetcher;

    @Autowired
    private KlineDataFiller klineDataFiller;

    @Autowired
    private MarketCalibrationTaskLogService taskLogService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public TaskExecutionResult execute(MarketCalibrationTaskConfig taskConfig, 
                                      LocalDateTime startTime, LocalDateTime endTime) {
        long startTimeMs = System.currentTimeMillis();
        MarketCalibrationTaskLog taskLog = null;
        TaskExecutionResult result = new TaskExecutionResult();

        try {
            log.info("开始执行缺失数据检测任务: taskConfigId={}, taskName={}, startTime={}, endTime={}", 
                    taskConfig.getId(), taskConfig.getTaskName(), startTime, endTime);

            // 1. 获取交易对信息
            TradingPairInfo pairInfo = tradingPairInfoService.getTradingPairInfo(taskConfig.getTradingPairId());
            String symbol = pairInfo.getSymbol();
            String symbolOnExchange = pairInfo.getSymbolOnExchange();

            // 2. 创建执行日志（状态=RUNNING）
            TaskLogCreateRequest logCreateRequest = new TaskLogCreateRequest();
            logCreateRequest.setTaskConfigId(taskConfig.getId());
            logCreateRequest.setTaskName(taskConfig.getTaskName());
            logCreateRequest.setTaskType(taskConfig.getTaskType());
            logCreateRequest.setTradingPairId(taskConfig.getTradingPairId());
            logCreateRequest.setSymbol(symbol);
            logCreateRequest.setExecutionMode(taskConfig.getExecutionMode());
            logCreateRequest.setDetectStartTime(startTime);
            logCreateRequest.setDetectEndTime(endTime);
            logCreateRequest.setStatus(TaskLogStatus.RUNNING);
            taskLog = taskLogService.createLog(logCreateRequest);

            // 3. 将LocalDateTime转换为UTC epoch millis
            // 注意：用户输入的时间字符串应该直接当作UTC时间来处理，不需要时区转换
            java.time.ZoneId utcZone = java.time.ZoneId.of("UTC");
            long startTimestamp = startTime.atZone(utcZone).toInstant().toEpochMilli();
            long endTimestamp = endTime.atZone(utcZone).toInstant().toEpochMilli();
            
            // 检测缺失的K线时间点（使用UTC epoch millis）
            List<Long> missingTimestamps = klineGapDetector.detectMissingTimestamps(
                    taskConfig.getTradingPairId(), startTimestamp, endTimestamp);
            result.setMissingCount(missingTimestamps.size());

            log.info("检测到缺失的K线时间点: taskConfigId={}, 缺失数量={}", 
                    taskConfig.getId(), missingTimestamps.size());

            int filledCount = 0;
            if (!missingTimestamps.isEmpty()) {
                // 4. 从OKX API拉取数据
                List<NormalizedKline> klines = historicalKlineFetcher.fetchHistoricalKlines(
                        symbolOnExchange, missingTimestamps);
                log.info("从OKX API拉取K线数据: taskConfigId={}, 拉取数量={}", 
                        taskConfig.getId(), klines.size());

                if (!klines.isEmpty()) {
                    // 5. 插入到QuestDB
                    filledCount = klineDataFiller.fillMissingKlines(symbolOnExchange, klines);
                    log.info("K线数据补全完成: taskConfigId={}, 补全数量={}", 
                            taskConfig.getId(), filledCount);
                }
            }
            
            // 设置填充数量到结果对象（确保始终有值）
            result.setFilledCount(filledCount);

            // 6. 构建执行日志详情（JSON格式）
            // 注意：不记录完整的时间戳列表，只记录统计信息，避免日志过大
            Map<String, Object> logDetails = new HashMap<>();
            logDetails.put("missingCount", missingTimestamps.size());
            logDetails.put("fetchedCount", missingTimestamps.isEmpty() ? 0 : filledCount);
            logDetails.put("filledCount", filledCount);
            // 只记录前100个缺失时间戳作为示例（如果数量很多）
            if (missingTimestamps.size() > 0 && missingTimestamps.size() <= 100) {
                logDetails.put("missingTimestamps", missingTimestamps);
            } else if (missingTimestamps.size() > 100) {
                logDetails.put("missingTimestampsSample", missingTimestamps.subList(0, 100));
                logDetails.put("totalMissingCount", missingTimestamps.size());
            }
            String executeLogJson = objectMapper.writeValueAsString(logDetails);

            // 7. 更新执行日志（状态=SUCCESS）
            long executeDurationMs = System.currentTimeMillis() - startTimeMs;
            result.setSuccess(true);
            result.setExecuteDurationMs(executeDurationMs);

            TaskLogUpdateRequest logUpdateRequest = new TaskLogUpdateRequest();
            logUpdateRequest.setStatus(TaskLogStatus.SUCCESS);
            logUpdateRequest.setMissingCount(result.getMissingCount());
            logUpdateRequest.setFilledCount(filledCount);
            logUpdateRequest.setExecuteDurationMs(executeDurationMs);
            logUpdateRequest.setExecuteLog(executeLogJson);
            taskLogService.updateLogStatus(taskLog.getId(), TaskLogStatus.SUCCESS, logUpdateRequest);

            log.info("缺失数据检测任务执行完成: taskConfigId={}, 缺失={}, 补全={}, 耗时={}ms", 
                    taskConfig.getId(), result.getMissingCount(), filledCount, executeDurationMs);

            return result;
        } catch (Exception e) {
            log.error("缺失数据检测任务执行失败: taskConfigId={}", taskConfig.getId(), e);
            
            // 更新执行日志（状态=FAILED）
            long executeDurationMs = System.currentTimeMillis() - startTimeMs;
            result.setSuccess(false);
            result.setExecuteDurationMs(executeDurationMs);
            result.setErrorMessage(e.getMessage());

            if (taskLog != null) {
                try {
                    TaskLogUpdateRequest logUpdateRequest = new TaskLogUpdateRequest();
                    logUpdateRequest.setStatus(TaskLogStatus.FAILED);
                    logUpdateRequest.setExecuteDurationMs(executeDurationMs);
                    logUpdateRequest.setErrorMessage(e.getMessage());
                    taskLogService.updateLogStatus(taskLog.getId(), TaskLogStatus.FAILED, logUpdateRequest);
                } catch (Exception ex) {
                    log.error("更新执行日志失败: taskLogId={}", taskLog.getId(), ex);
                }
            }

            return result;
        }
    }
}

