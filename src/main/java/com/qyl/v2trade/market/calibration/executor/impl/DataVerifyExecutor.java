package com.qyl.v2trade.market.calibration.executor.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qyl.v2trade.client.OkxApiClient;
import com.qyl.v2trade.common.util.UtcTimeConverter;
import com.qyl.v2trade.market.calibration.common.TaskLogStatus;
import com.qyl.v2trade.market.calibration.executor.MarketCalibrationExecutor;
import com.qyl.v2trade.market.calibration.executor.dto.TaskExecutionResult;
import com.qyl.v2trade.market.calibration.config.entity.MarketCalibrationTaskConfig;
import com.qyl.v2trade.market.calibration.log.dto.TaskLogCreateRequest;
import com.qyl.v2trade.market.calibration.log.dto.TaskLogUpdateRequest;
import com.qyl.v2trade.market.calibration.log.entity.MarketCalibrationTaskLog;
import com.qyl.v2trade.market.calibration.log.service.MarketCalibrationTaskLogService;
import com.qyl.v2trade.market.calibration.service.TradingPairInfoService;
import com.qyl.v2trade.market.calibration.service.dto.TradingPairInfo;
import com.qyl.v2trade.market.calibration.util.KlineTimeCalculator;
import com.qyl.v2trade.market.model.NormalizedKline;
import com.qyl.v2trade.market.web.query.MarketQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据核对执行器
 */
@Slf4j
@Component
public class DataVerifyExecutor implements MarketCalibrationExecutor {

    @Autowired
    private TradingPairInfoService tradingPairInfoService;

    @Autowired
    @Qualifier("questDbMarketQueryService")
    private MarketQueryService marketQueryService;

    @Autowired
    private MarketCalibrationTaskLogService taskLogService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OkxApiClient okxApiClient;

    @Override
    public TaskExecutionResult execute(MarketCalibrationTaskConfig taskConfig,
                                       LocalDateTime startTime, LocalDateTime endTime) {
        long startTimeMs = System.currentTimeMillis();
        MarketCalibrationTaskLog taskLog = null;
        TaskExecutionResult result = new TaskExecutionResult();

        try {
            log.info("开始执行数据核对任务: taskConfigId={}, taskName={}, startTime={}, endTime={}",
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
            ZoneId utcZone = ZoneId.of("UTC");
            long startTimestamp = startTime.atZone(utcZone).toInstant().toEpochMilli();
            long endTimestamp = endTime.atZone(utcZone).toInstant().toEpochMilli();

            // 对齐时间戳到分钟起始点
            long alignedStartTimestamp = KlineTimeCalculator.alignToMinuteStart(startTimestamp);
            long alignedEndTimestamp = KlineTimeCalculator.alignToMinuteStart(endTimestamp);

            // 4. 从QuestDB查询已存储的K线数据
            List<NormalizedKline> questDbKlines = marketQueryService.queryKlines(
                    symbolOnExchange, "1m", alignedStartTimestamp, alignedEndTimestamp, null);
            log.info("从QuestDB查询到K线数据: taskConfigId={}, 数量={}", taskConfig.getId(), questDbKlines.size());

            // 5. 从OKX交易所获取K线数据
            List<NormalizedKline> okxKlines = fetchKlinesFromOkx(symbolOnExchange, alignedStartTimestamp, alignedEndTimestamp);
            log.info("从OKX获取到K线数据: taskConfigId={}, 数量={}", taskConfig.getId(), okxKlines.size());

            // 6. 对比QuestDB和OKX的数据，找出QuestDB缺失的K线
            List<MissingKlineInfo> missingKlines = compareWithQuestDb(questDbKlines, okxKlines);
            log.info("检测到QuestDB缺失的K线: taskConfigId={}, 缺失数量={}", taskConfig.getId(), missingKlines.size());

            // 7. 执行数据核对检查（基于QuestDB的数据）
            VerifyResult verifyResult = performVerification(questDbKlines);

            // 8. 构建执行日志详情（JSON格式）
            // 注意：限制详细列表的大小，避免日志过大（最多保留前100条）
            Map<String, Object> logDetails = new HashMap<>();
            logDetails.put("duplicateCount", verifyResult.duplicates.size());
            logDetails.put("outOfOrderCount", verifyResult.outOfOrder.size());
            logDetails.put("dataErrorCount", verifyResult.dataErrors.size());
            logDetails.put("missingKlineCount", missingKlines.size());

            // 记录缺失的K线信息（转换为UTC时间字符串）
            if (!missingKlines.isEmpty()) {
                List<Map<String, Object>> missingKlineList = missingKlines.stream()
                        .map(missing -> {
                            Map<String, Object> info = new HashMap<>();
                            info.put("timestamp", missing.timestamp);
                            info.put("utcTime", UtcTimeConverter.utcTimestampToUtcString(missing.timestamp));
                            return info;
                        })
                        .collect(Collectors.toList());

                // 只保留前100条作为示例
                int maxDetailSize = 100;
                if (missingKlineList.size() > maxDetailSize) {
                    logDetails.put("missingKlines", missingKlineList.subList(0, maxDetailSize));
                    logDetails.put("totalMissingKlineCount", missingKlineList.size());
                } else {
                    logDetails.put("missingKlines", missingKlineList);
                }
            }

            // 只保留前100条作为示例
            int maxDetailSize = 100;
            if (verifyResult.duplicates.size() > 0) {
                logDetails.put("duplicates", verifyResult.duplicates.size() > maxDetailSize
                        ? verifyResult.duplicates.subList(0, maxDetailSize)
                        : verifyResult.duplicates);
                if (verifyResult.duplicates.size() > maxDetailSize) {
                    logDetails.put("totalDuplicateCount", verifyResult.duplicates.size());
                }
            }
            if (verifyResult.outOfOrder.size() > 0) {
                logDetails.put("outOfOrder", verifyResult.outOfOrder.size() > maxDetailSize
                        ? verifyResult.outOfOrder.subList(0, maxDetailSize)
                        : verifyResult.outOfOrder);
                if (verifyResult.outOfOrder.size() > maxDetailSize) {
                    logDetails.put("totalOutOfOrderCount", verifyResult.outOfOrder.size());
                }
            }
            if (verifyResult.dataErrors.size() > 0) {
                logDetails.put("dataErrors", verifyResult.dataErrors.size() > maxDetailSize
                        ? verifyResult.dataErrors.subList(0, maxDetailSize)
                        : verifyResult.dataErrors);
                if (verifyResult.dataErrors.size() > maxDetailSize) {
                    logDetails.put("totalDataErrorCount", verifyResult.dataErrors.size());
                }
            }
            String executeLogJson = objectMapper.writeValueAsString(logDetails);

            // 9. 更新执行日志（状态、统计信息、详细日志）
            long executeDurationMs = System.currentTimeMillis() - startTimeMs;
            result.setSuccess(true);
            result.setExecuteDurationMs(executeDurationMs);
            result.setDuplicateCount(verifyResult.duplicates.size());
            result.setErrorCount(verifyResult.dataErrors.size() + verifyResult.outOfOrder.size());

            TaskLogUpdateRequest logUpdateRequest = new TaskLogUpdateRequest();
            logUpdateRequest.setStatus(TaskLogStatus.SUCCESS);
            logUpdateRequest.setDuplicateCount(result.getDuplicateCount());
            logUpdateRequest.setErrorCount(result.getErrorCount());
            logUpdateRequest.setExecuteDurationMs(executeDurationMs);
            logUpdateRequest.setExecuteLog(executeLogJson);
            taskLogService.updateLogStatus(taskLog.getId(), TaskLogStatus.SUCCESS, logUpdateRequest);

            log.info("数据核对任务执行完成: taskConfigId={}, QuestDB数量={}, OKX数量={}, 缺失={}, 重复={}, 异常={}, 耗时={}ms",
                    taskConfig.getId(), questDbKlines.size(), okxKlines.size(),
                    missingKlines.size(), result.getDuplicateCount(), result.getErrorCount(), executeDurationMs);

            return result;
        } catch (Exception e) {
            log.error("数据核对任务执行失败: taskConfigId={}", taskConfig.getId(), e);

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

    /**
     * 执行数据核对检查
     */
    private VerifyResult performVerification(List<NormalizedKline> klines) {
        VerifyResult result = new VerifyResult();

        if (klines == null || klines.isEmpty()) {
            return result;
        }

        // 1. 重复检测：同一时间戳是否有多条数据
        Map<Long, List<NormalizedKline>> timestampMap = klines.stream()
                .collect(Collectors.groupingBy(NormalizedKline::getTimestamp));

        timestampMap.forEach((timestamp, list) -> {
            if (list.size() > 1) {
                Map<String, Object> duplicate = new HashMap<>();
                duplicate.put("timestamp", timestamp);
                duplicate.put("count", list.size());
                result.duplicates.add(duplicate);
            }
        });

        // 2. 时间顺序检测：检查是否有倒序
        for (int i = 1; i < klines.size(); i++) {
            NormalizedKline prev = klines.get(i - 1);
            NormalizedKline curr = klines.get(i);
            if (curr.getTimestamp() < prev.getTimestamp()) {
                Map<String, Object> outOfOrder = new HashMap<>();
                outOfOrder.put("timestamp1", prev.getTimestamp());
                outOfOrder.put("timestamp2", curr.getTimestamp());
                result.outOfOrder.add(outOfOrder);
            }
        }

        // 3. 数据异常检测
        for (NormalizedKline kline : klines) {
            List<String> errors = new ArrayList<>();

            // 价格/成交量为负数
            if (kline.getOpen() < 0 || kline.getHigh() < 0 || kline.getLow() < 0 || kline.getClose() < 0) {
                errors.add("NEGATIVE_PRICE");
            }
            if (kline.getVolume() < 0) {
                errors.add("NEGATIVE_VOLUME");
            }

            // 价格/成交量过大（这里设置一个合理的上限，比如100万美元）
            double maxReasonablePrice = 1000000.0;
            if (kline.getOpen() > maxReasonablePrice || kline.getHigh() > maxReasonablePrice ||
                    kline.getLow() > maxReasonablePrice || kline.getClose() > maxReasonablePrice) {
                errors.add("PRICE_TOO_HIGH");
            }

            // open/high/low/close 逻辑错误
            if (kline.getHigh() < kline.getLow()) {
                errors.add("HIGH_LESS_THAN_LOW");
            }
            if (kline.getHigh() < kline.getOpen() || kline.getHigh() < kline.getClose()) {
                errors.add("HIGH_INVALID");
            }
            if (kline.getLow() > kline.getOpen() || kline.getLow() > kline.getClose()) {
                errors.add("LOW_INVALID");
            }

            if (!errors.isEmpty()) {
                Map<String, Object> dataError = new HashMap<>();
                dataError.put("timestamp", kline.getTimestamp());
                dataError.put("errorType", String.join(",", errors));
                dataError.put("open", kline.getOpen());
                dataError.put("high", kline.getHigh());
                dataError.put("low", kline.getLow());
                dataError.put("close", kline.getClose());
                dataError.put("volume", kline.getVolume());
                result.dataErrors.add(dataError);
            }
        }

        return result;
    }

    /**
     * 从OKX交易所获取K线数据
     *
     * <p>根据时间范围判断使用哪个接口：
     * <ul>
     *   <li>如果结束时间戳是今天（UTC时间），使用 getKlines() 方法（/api/v5/market/candles）</li>
     *   <li>如果结束时间戳不是今天，使用 getHistoryCandles() 方法（/api/v5/market/history-candles）</li>
     * </ul>
     *
     * @param symbolOnExchange 交易所格式的symbol（如：BTC-USDT-SWAP）
     * @param startTimestamp   开始时间戳（毫秒，UTC epoch millis，已对齐到分钟起始点）
     * @param endTimestamp     结束时间戳（毫秒，UTC epoch millis，已对齐到分钟起始点）
     * @return K线数据列表
     */
    private List<NormalizedKline> fetchKlinesFromOkx(String symbolOnExchange, long startTimestamp, long endTimestamp) {
        try {

            log.info("开始从OKX获取K线数据: symbol={}, start={} (UTC: {}), end={} (UTC: {})",
                    symbolOnExchange, startTimestamp, UtcTimeConverter.utcTimestampToUtcString(startTimestamp),
                    endTimestamp, UtcTimeConverter.utcTimestampToUtcString(endTimestamp));

            // 判断结束时间戳是否是今天（UTC时间）
            boolean isEndTimeToday = isTodayInUTC(endTimestamp);

            // 根据是否是今天选择不同的接口
            JsonNode response;
            if (isEndTimeToday) {
                log.info("结束时间戳是今天，使用 /api/v5/market/candles 接口");
                // 调用OKX API获取K线数据（今天的数据）
                // OKX API参数说明：
                // after: 请求此时间戳之后的数据（更旧的历史数据）
                // before: 请求此时间戳之前的数据（更新的数据，接近当前时间）
                // 注意：OKX返回的数据是按时间倒序排列的（最新的在前）
                response = okxApiClient.getKlines(symbolOnExchange, "1m", startTimestamp, endTimestamp, 300);
            } else {
                log.info("结束时间戳不是今天，使用 /api/v5/market/history-candles 接口");
                // 调用OKX历史K线API获取数据（非今天的数据）
                response = okxApiClient.getHistoryCandles(symbolOnExchange, "1m", startTimestamp, endTimestamp, 300);
            }

            // 解析响应
            if (response == null || !response.has("code")) {
                log.warn("OKX API返回无效响应: symbol={}, isToday={}", symbolOnExchange, isEndTimeToday);
                return new ArrayList<>();
            }

            String code = response.get("code").asText();
            if (!"0".equals(code)) {
                String msg = response.has("msg") ? response.get("msg").asText() : "未知错误";
                log.warn("OKX API返回错误: code={}, msg={}, symbol={}, isToday={}",
                        code, msg, symbolOnExchange, isEndTimeToday);
                return new ArrayList<>();
            }

            JsonNode dataArray = response.get("data");
            if (dataArray == null || !dataArray.isArray()) {
                log.warn("OKX API返回数据格式错误: symbol={}, isToday={}", symbolOnExchange, isEndTimeToday);
                return new ArrayList<>();
            }

            // 解析K线数据
            // OKX返回格式：["1698768000000","42600.1","42600.1","42600","42600","0.12345","0.12345","1698768059999"]
            // [时间戳(毫秒), 开盘价, 最高价, 最低价, 收盘价, 成交量, 成交额, K线结束时间戳(毫秒)]
            List<NormalizedKline> klines = new ArrayList<>();
            for (JsonNode candle : dataArray) {
                if (candle.isArray() && candle.size() >= 6) {
                    try {
                        long timestamp = Long.parseLong(candle.get(0).asText());
                        // 对齐到分钟起始点
                        long alignedTimestamp = KlineTimeCalculator.alignToMinuteStart(timestamp);

                        // 只处理在时间范围内的数据
                        if (alignedTimestamp >= startTimestamp && alignedTimestamp < endTimestamp) {
                            NormalizedKline kline = NormalizedKline.builder()
                                    .symbol(symbolOnExchange)
                                    .interval("1m")
                                    .open(Double.parseDouble(candle.get(1).asText()))
                                    .high(Double.parseDouble(candle.get(2).asText()))
                                    .low(Double.parseDouble(candle.get(3).asText()))
                                    .close(Double.parseDouble(candle.get(4).asText()))
                                    .volume(Double.parseDouble(candle.get(5).asText()))
                                    .build();
                            // 使用兼容性方法设置时间戳（long -> Instant）
                            kline.setTimestamp(alignedTimestamp);
                            kline.setExchangeTimestamp(timestamp);
                            klines.add(kline);
                        }
                    } catch (Exception e) {
                        log.warn("解析OKX K线数据失败: candle={}, error={}", candle.toString(), e.getMessage());
                    }
                }
            }

            log.info("从OKX获取K线数据完成: symbol={}, 接口类型={}, 原始数量={}, 范围内有效数量={}",
                    symbolOnExchange, isEndTimeToday ? "candles" : "history-candles",
                    dataArray.size(), klines.size());
            return klines;

        } catch (Exception e) {
            log.error("从OKX获取K线数据失败: symbol={}, startTimestamp={}, endTimestamp={}",
                    symbolOnExchange, startTimestamp, endTimestamp, e);
            // 返回空列表，不中断整个校准流程
            return new ArrayList<>();
        }
    }

    /**
     * 判断时间戳是否是今天（UTC时间）
     *
     * @param timestamp 时间戳（毫秒，UTC epoch millis）
     * @return 是否是今天
     */
    private boolean isTodayInUTC(long timestamp) {
        java.time.Instant timestampInstant = java.time.Instant.ofEpochMilli(timestamp);
        java.time.LocalDate timestampDate = timestampInstant.atZone(java.time.ZoneOffset.UTC).toLocalDate();

        java.time.Instant nowInstant = java.time.Instant.now();
        java.time.LocalDate todayDate = nowInstant.atZone(java.time.ZoneOffset.UTC).toLocalDate();

        return timestampDate.equals(todayDate);
    }

    /**
     * 对比QuestDB和OKX的数据，找出QuestDB缺失的K线
     *
     * @param questDbKlines QuestDB中的K线数据
     * @param okxKlines     OKX交易所的K线数据
     * @return 缺失的K线信息列表（包含时间戳）
     */
    private List<MissingKlineInfo> compareWithQuestDb(List<NormalizedKline> questDbKlines,
                                                      List<NormalizedKline> okxKlines) {
        List<MissingKlineInfo> missingKlines = new ArrayList<>();

        if (okxKlines == null || okxKlines.isEmpty()) {
            return missingKlines;
        }

        // 提取QuestDB中已存在的时间戳集合（对齐到分钟起始点）
        Set<Long> questDbTimestamps = questDbKlines.stream()
                .map(kline -> KlineTimeCalculator.alignToMinuteStart(kline.getTimestamp()))
                .collect(Collectors.toSet());

        // 找出OKX有但QuestDB没有的K线
        for (NormalizedKline okxKline : okxKlines) {
            long alignedTimestamp = KlineTimeCalculator.alignToMinuteStart(okxKline.getTimestamp());
            if (!questDbTimestamps.contains(alignedTimestamp)) {
                missingKlines.add(new MissingKlineInfo(alignedTimestamp));
            }
        }

        // 按时间戳排序
        missingKlines.sort((a, b) -> Long.compare(a.timestamp, b.timestamp));

        return missingKlines;
    }

    /**
     * 缺失K线信息内部类
     */
    private static class MissingKlineInfo {
        final long timestamp;

        MissingKlineInfo(long timestamp) {
            this.timestamp = timestamp;
        }
    }

    /**
     * 核对结果内部类
     */
    private static class VerifyResult {
        List<Map<String, Object>> duplicates = new ArrayList<>();
        List<Map<String, Object>> outOfOrder = new ArrayList<>();
        List<Map<String, Object>> dataErrors = new ArrayList<>();
    }
}

