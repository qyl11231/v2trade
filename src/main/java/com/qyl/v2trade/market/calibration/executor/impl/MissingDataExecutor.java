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

import com.qyl.v2trade.common.util.TimeUtil;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 缺失数据检测执行器
 * 
 * <p>重构：使用 Instant 作为时间参数，遵循 UTC Everywhere 原则
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
                                      Instant startTime, Instant endTime) {
        long startTimeMs = System.currentTimeMillis();
        MarketCalibrationTaskLog taskLog = null;
        TaskExecutionResult result = new TaskExecutionResult();

        try {
            log.info("开始执行缺失数据检测任务: taskConfigId={}, taskName={}, startTime={}, endTime={}", 
                    taskConfig.getId(), taskConfig.getTaskName(), 
                    TimeUtil.formatWithBothTimezones(startTime), 
                    TimeUtil.formatWithBothTimezones(endTime));

            // 1. 获取交易对信息
            TradingPairInfo pairInfo = tradingPairInfoService.getTradingPairInfo(taskConfig.getTradingPairId());
            String symbol = pairInfo.getSymbol();
            String symbolOnExchange = pairInfo.getSymbolOnExchange();

            // 2. 创建执行日志（状态=RUNNING）
            // 重构：将 Instant 转换为 LocalDateTime 用于数据库存储（数据库边界转换）
            TaskLogCreateRequest logCreateRequest = new TaskLogCreateRequest();
            logCreateRequest.setTaskConfigId(taskConfig.getId());
            logCreateRequest.setTaskName(taskConfig.getTaskName());
            logCreateRequest.setTaskType(taskConfig.getTaskType());
            logCreateRequest.setTradingPairId(taskConfig.getTradingPairId());
            logCreateRequest.setSymbol(symbol);
            logCreateRequest.setExecutionMode(taskConfig.getExecutionMode());
            // 将 Instant 转换为 LocalDateTime（UTC），用于数据库存储
            logCreateRequest.setDetectStartTime(startTime.atZone(ZoneOffset.UTC).toLocalDateTime());
            logCreateRequest.setDetectEndTime(endTime.atZone(ZoneOffset.UTC).toLocalDateTime());
            logCreateRequest.setStatus(TaskLogStatus.RUNNING);
            taskLog = taskLogService.createLog(logCreateRequest);

            // 3. 将 Instant 转换为 UTC epoch millis
            // 检测缺失的K线时间点
            // 重构：使用 Instant 参数，遵循时间管理约定
            List<Long> missingTimestamps = klineGapDetector.detectMissingTimestamps(
                    taskConfig.getTradingPairId(), startTime, endTime);
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

