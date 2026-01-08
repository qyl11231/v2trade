package com.qyl.v2trade.indicator.calculator;

import com.qyl.v2trade.indicator.domain.event.BarClosedEvent;
import com.qyl.v2trade.indicator.engine.IndicatorComputeRequest;
import com.qyl.v2trade.indicator.engine.IndicatorEngine;
import com.qyl.v2trade.indicator.engine.IndicatorResult;
import com.qyl.v2trade.indicator.definition.IndicatorDefinition;
import com.qyl.v2trade.indicator.definition.IndicatorRegistry;
import com.qyl.v2trade.indicator.observability.IndicatorMetrics;
import com.qyl.v2trade.indicator.persistence.CalcFingerprint;
import com.qyl.v2trade.indicator.repository.IndicatorCalcLogRepository;
import com.qyl.v2trade.indicator.repository.IndicatorSubscriptionRepository;
import com.qyl.v2trade.indicator.repository.IndicatorValueRepository;
import com.qyl.v2trade.indicator.repository.IndicatorValueRepository.WriteResult;
import com.qyl.v2trade.indicator.repository.entity.IndicatorCalcLog;
import com.qyl.v2trade.indicator.repository.entity.IndicatorSubscription;
import com.qyl.v2trade.indicator.repository.entity.IndicatorValue;
import com.qyl.v2trade.indicator.series.BarSeriesManager;
import com.qyl.v2trade.indicator.series.BarSeriesView;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 指标计算器
 * 
 * <p>职责：
 * 1. 监听BarClosedEvent（handler：快速投递任务，不阻塞）
 * 2. 根据订阅查询需要计算的指标
 * 3. 路由到对应的引擎进行计算
 * 
 * <p>规则：
 * - 每次BAR_CLOSED对该pair/timeframe只计算订阅指标
 * - handler不阻塞行情线程（使用异步处理）
 *
 * @author qyl
 */
@Slf4j
@Component
public class IndicatorCalculator {
    
    @Autowired
    private IndicatorSubscriptionRepository subscriptionRepository;
    
    @Autowired
    private IndicatorRegistry indicatorRegistry;
    
    @Autowired
    private IndicatorEngineRouter engineRouter;
    
    @Autowired
    private BarSeriesManager barSeriesManager;
    
    @Autowired
    private IndicatorValueRepository valueRepository;
    
    @Autowired
    private IndicatorCalcLogRepository calcLogRepository;
    
    @Autowired
    private IndicatorMetrics metrics;
    
    /**
     * 处理BarClosedEvent（handler：快速投递任务）
     * 
     * <p>不阻塞，使用异步处理
     */
    @EventListener
    @Async("indicatorCalculatorExecutor")
    public void onBarClosed(BarClosedEvent event) {
        if (event == null || event.tradingPairId() == null || event.timeframe() == null) {
            log.warn("收到无效的BarClosedEvent: {}", event);
            return;
        }
        
        // 过滤：只处理支持的周期（5m、15m、30m、1h、4h），不处理1m
        if (!com.qyl.v2trade.indicator.infrastructure.time.QuestDbTsSemanticsProbe
                .isTimeframeSupported(event.timeframe())) {
            log.debug("跳过不支持的周期: pairId={}, timeframe={} (指标模块只支持5m/15m/30m/1h/4h)",
                    event.tradingPairId(), event.timeframe());
            return;
        }
        
        log.debug("收到BarClosedEvent，准备计算指标: pairId={}, timeframe={}, barTime={}",
                event.tradingPairId(), event.timeframe(), event.barCloseTime());
        
        // 查询所有订阅了该pair/timeframe的订阅（包含userId信息）
        List<IndicatorSubscription> subscriptions = subscriptionRepository.findByPairAndTimeframe(
                event.tradingPairId(), event.timeframe());
        
        if (subscriptions.isEmpty()) {
            log.debug("没有找到订阅，跳过计算: pairId={}, timeframe={}",
                    event.tradingPairId(), event.timeframe());
            return;
        }
        
        // 按userId分组处理
        Map<Long, List<IndicatorSubscription>> subscriptionsByUser = subscriptions.stream()
                .filter(sub -> sub.getEnabled() != null && sub.getEnabled() == 1)
                .collect(java.util.stream.Collectors.groupingBy(IndicatorSubscription::getUserId));
        
        // 对每个用户进行计算
        for (Map.Entry<Long, List<IndicatorSubscription>> entry : subscriptionsByUser.entrySet()) {
            Long userId = entry.getKey();
            
            // 执行计算
            computeForPairTfBar(userId, event.tradingPairId(), event.timeframe(), event.barCloseTime());
        }
    }
    
    /**
     * 计算指定pair/timeframe/barTime的指标（worker执行）
     * 
     * @param userId 用户ID
     * @param pairId 交易对ID
     * @param timeframe 周期
     * @param barTime Bar收盘时间
     */
    public void computeForPairTfBar(long userId, long pairId, String timeframe, LocalDateTime barTime) {
        try {
            log.debug("开始计算指标: userId={}, pairId={}, timeframe={}, barTime={}",
                    userId, pairId, timeframe, barTime);
            
            // 1. 查询该pair/timeframe的订阅
            List<IndicatorSubscription> subscriptions = subscriptionRepository.findByUserAndPairAndTimeframe(
                    userId, pairId, timeframe);
            
            if (subscriptions.isEmpty()) {
                log.debug("用户没有订阅，跳过计算: userId={}, pairId={}, timeframe={}",
                        userId, pairId, timeframe);
                return;
            }
            
            // 2. 获取BarSeries（K线数据是共享的，不区分用户）
            BarSeriesView series = barSeriesManager.getSeries(pairId, timeframe);
            if (series == null || series.size() == 0) {
                log.warn("BarSeries为空，跳过计算: userId={}, pairId={}, timeframe={}",
                        userId, pairId, timeframe);
                return;
            }
            
            // 3. 对每个订阅的指标进行计算
            for (IndicatorSubscription subscription : subscriptions) {
                if (subscription.getEnabled() == null || subscription.getEnabled() != 1) {
                    continue;
                }
                
                computeIndicator(userId, subscription, series, barTime);
            }
            
        } catch (Exception e) {
            log.error("计算指标失败: userId={}, pairId={}, timeframe={}, barTime={}",
                    userId, pairId, timeframe, barTime, e);
        }
    }
    
    /**
     * 计算单个指标（阶段5：包含落库逻辑）
     */
    private void computeIndicator(
            long userId,
            IndicatorSubscription subscription,
            BarSeriesView series,
            LocalDateTime barTime) {
        
        long startTime = System.currentTimeMillis();
        String indicatorCode = subscription.getIndicatorCode();
        String indicatorVersion = subscription.getIndicatorVersion();
        String engineName = null;
        IndicatorResult result = null;
        String errorMsg = null;
        
        try {
            // 1. 从Registry获取指标定义
            IndicatorDefinition definition = indicatorRegistry.getDefinition(indicatorCode, indicatorVersion);
            if (definition == null) {
                errorMsg = "找不到指标定义: code=" + indicatorCode + ", version=" + indicatorVersion;
                log.warn(errorMsg);
                int costMs = (int)(System.currentTimeMillis() - startTime);
                writeCalcLog(userId, subscription, barTime, null, "FAILED", costMs, errorMsg, null);
                metrics.recordFail(indicatorCode, null);
                metrics.recordCalcCost(costMs);
                return;
            }
            
            // 2. 获取引擎
            engineName = definition.engine();
            IndicatorEngine engine = engineRouter.getEngine(engineName);
            if (engine == null) {
                errorMsg = "找不到引擎: engineName=" + engineName + ", indicatorCode=" + indicatorCode;
                log.warn(errorMsg);
                int costMs = (int)(System.currentTimeMillis() - startTime);
                writeCalcLog(userId, subscription, barTime, engineName, "FAILED", costMs, errorMsg, null);
                metrics.recordFail(indicatorCode, engineName);
                metrics.recordCalcCost(costMs);
                return;
            }
            
            // 3. 构建计算请求
            IndicatorComputeRequest request = new IndicatorComputeRequest(
                    indicatorCode,
                    indicatorVersion,
                    subscription.getParams(),
                    subscription.getTradingPairId(),
                    subscription.getTimeframe(),
                    barTime
            );
            
            // 4. 执行计算
            result = engine.compute(request, series);
            
            // 5. 计算耗时
            int costMs = (int)(System.currentTimeMillis() - startTime);
            
            // 6. 记录Metrics（阶段6）
            metrics.recordCalcCost(costMs);
            
            // 7. 落库逻辑（阶段5）
            if (result.status() == IndicatorResult.Status.SUCCESS) {
                // 7.1 生成计算指纹
                String fingerprint = CalcFingerprint.generate(
                        indicatorCode, indicatorVersion, subscription.getParams(), engineName);
                
                // 7.2 写入calc_log（SUCCESS）
                writeCalcLog(userId, subscription, barTime, engineName, "SUCCESS", costMs, null, fingerprint);
                
                // 7.3 写入indicator_value（insert ignore）
                IndicatorValueRepository.WriteResult writeResult = writeIndicatorValue(
                        userId, subscription, barTime, result, engineName, fingerprint, costMs);
                
                // 7.4 冲突检测（如果被ignore）
                if (writeResult == IndicatorValueRepository.WriteResult.IGNORED) {
                    boolean hasConflict = checkConflict(userId, subscription, barTime, result, fingerprint);
                    if (hasConflict) {
                        metrics.recordConflict(indicatorCode, engineName);
                    }
                }
                
                // 7.5 结构化日志（阶段6）
                logStructuredCalcResult(userId, subscription, barTime, engineName, "SUCCESS", 
                        costMs, fingerprint, result.values(), null);
            } else {
                // 计算失败，只写calc_log（FAILED）
                errorMsg = result.errorMessage();
                writeCalcLog(userId, subscription, barTime, engineName, "FAILED", costMs, errorMsg, null);
                metrics.recordFail(indicatorCode, engineName);
                
                // 结构化日志（阶段6）
                logStructuredCalcResult(userId, subscription, barTime, engineName, "FAILED", 
                        costMs, null, null, errorMsg);
            }
            
        } catch (Exception e) {
            // 异常情况，写FAILED日志
            errorMsg = "计算异常: " + e.getMessage();
            int costMs = (int)(System.currentTimeMillis() - startTime);
            writeCalcLog(userId, subscription, barTime, engineName, "FAILED", costMs, errorMsg, null);
            metrics.recordFail(indicatorCode, engineName);
            metrics.recordCalcCost(costMs);
            
            // 结构化日志（阶段6）
            logStructuredCalcResult(userId, subscription, barTime, engineName, "FAILED", 
                    costMs, null, null, errorMsg);
        }
    }
    
    /**
     * 写入计算日志
     */
    private void writeCalcLog(long userId, IndicatorSubscription subscription, LocalDateTime barTime,
                              String engineName, String status, int costMs, String errorMsg, String fingerprint) {
        try {
            IndicatorCalcLog log = new IndicatorCalcLog();
            log.setUserId(userId);
            log.setTradingPairId(subscription.getTradingPairId());
            log.setSymbol(subscription.getSymbol());
            log.setMarketType(subscription.getMarketType());
            log.setTimeframe(subscription.getTimeframe());
            log.setBarTime(barTime);
            log.setIndicatorCode(subscription.getIndicatorCode());
            log.setIndicatorVersion(subscription.getIndicatorVersion());
            log.setCalcEngine(engineName);
            log.setStatus(status);
            log.setCostMs(costMs);
            log.setErrorMsg(errorMsg);
            
            calcLogRepository.append(log);
        } catch (Exception e) {
            log.error("写入计算日志失败: userId={}, indicatorCode={}, status={}",
                    userId, subscription.getIndicatorCode(), status, e);
        }
    }
    
    /**
     * 写入指标值
     */
    private WriteResult writeIndicatorValue(
            long userId, IndicatorSubscription subscription, LocalDateTime barTime,
            IndicatorResult result, String engineName, String fingerprint, int costMs) {
        
        try {
            IndicatorValue value = new IndicatorValue();
            value.setUserId(userId);
            value.setTradingPairId(subscription.getTradingPairId());
            value.setSymbol(subscription.getSymbol());
            value.setMarketType(subscription.getMarketType());
            value.setTimeframe(subscription.getTimeframe());
            value.setBarTime(barTime);
            value.setIndicatorCode(subscription.getIndicatorCode());
            value.setIndicatorVersion(subscription.getIndicatorVersion());
            value.setCalcEngine(engineName);
            value.setCalcFingerprint(fingerprint);
            value.setCalcCostMs(costMs);
            value.setSource("OKX"); // 默认数据源
            value.setDataQuality("OK"); // 默认数据质量
            
            // 设置指标值（单值或多值）
            Map<String, BigDecimal> resultValues = result.values();
            if (resultValues != null && !resultValues.isEmpty()) {
                if (resultValues.size() == 1) {
                    // 单值指标
                    value.setValue(resultValues.values().iterator().next());
                    value.setExtraValues(null);
                } else {
                    // 多值指标
                    value.setValue(null);
                    value.setExtraValues(resultValues);
                }
            }
            
            return valueRepository.insertIgnore(value);
            
        } catch (Exception e) {
            log.error("写入指标值失败: userId={}, indicatorCode={}",
                    userId, subscription.getIndicatorCode(), e);
            return IndicatorValueRepository.WriteResult.IGNORED;
        }
    }
    
    /**
     * 结构化日志（阶段6）
     */
    private void logStructuredCalcResult(long userId, IndicatorSubscription subscription, 
                                        LocalDateTime barTime, String engineName, String status,
                                        int costMs, String fingerprint, Map<String, BigDecimal> values, 
                                        String errorMsg) {
        try {
            // 使用MDC记录结构化日志字段
            MDC.put("user_id", String.valueOf(userId));
            MDC.put("trading_pair_id", String.valueOf(subscription.getTradingPairId()));
            MDC.put("symbol", subscription.getSymbol());
            MDC.put("timeframe", subscription.getTimeframe());
            MDC.put("bar_time", barTime.toString());
            MDC.put("indicator_code", subscription.getIndicatorCode());
            MDC.put("indicator_version", subscription.getIndicatorVersion());
            MDC.put("engine", engineName != null ? engineName : "");
            MDC.put("cost_ms", String.valueOf(costMs));
            MDC.put("data_quality", "OK"); // 默认OK
            MDC.put("calc_fingerprint", fingerprint != null ? fingerprint : "");
            if (errorMsg != null) {
                MDC.put("error_msg", errorMsg);
            }
            
            // 记录日志
            if ("SUCCESS".equals(status)) {
                log.info("指标计算完成: status={}, indicatorCode={}, costMs={}ms", 
                        status, subscription.getIndicatorCode(), costMs);
            } else {
                log.warn("指标计算完成: status={}, indicatorCode={}, costMs={}ms, error={}", 
                        status, subscription.getIndicatorCode(), costMs, errorMsg);
            }
        } finally {
            // 清理MDC
            MDC.clear();
        }
    }
    
    /**
     * 冲突检测
     * 
     * @return 是否检测到冲突
     */
    private boolean checkConflict(long userId, IndicatorSubscription subscription, LocalDateTime barTime,
                               IndicatorResult result, String fingerprint) {
        
        try {
            // 查询已存在的记录
            java.util.Optional<IndicatorValue> existing = valueRepository.findOneKey(
                    userId,
                    subscription.getTradingPairId(),
                    subscription.getTimeframe(),
                    barTime,
                    subscription.getIndicatorCode(),
                    subscription.getIndicatorVersion()
            );
            
            if (!existing.isPresent()) {
                // 不存在记录（可能被其他线程删除了），不算冲突
                return false;
            }
            
            IndicatorValue existingValue = existing.get();
            
            // 检查指纹是否相同
            if (!fingerprint.equals(existingValue.getCalcFingerprint())) {
                // 指纹不同，检测值是否不同
                boolean valueDifferent = false;
                
                Map<String, BigDecimal> newValues = result.values();
                if (existingValue.getValue() != null) {
                    // 单值指标
                    if (newValues == null || newValues.isEmpty()) {
                        valueDifferent = true;
                    } else {
                        BigDecimal newValue = newValues.values().iterator().next();
                        if (newValue.compareTo(existingValue.getValue()) != 0) {
                            valueDifferent = true;
                        }
                    }
                } else if (existingValue.getExtraValues() != null) {
                    // 多值指标
                    if (newValues == null || !newValues.equals(existingValue.getExtraValues())) {
                        valueDifferent = true;
                    }
                }
                
                if (valueDifferent) {
                    // 值不同，记录冲突日志
                    String errorMsg = String.format("CONFLICT: fingerprint=%s vs %s, value mismatch",
                            fingerprint, existingValue.getCalcFingerprint());
                    
                    writeCalcLog(userId, subscription, barTime, existingValue.getCalcEngine(),
                            "FAILED", 0, errorMsg, fingerprint);
                    
                    log.warn("指标值冲突检测到差异: userId={}, indicatorCode={}, barTime={}, " +
                                    "newFingerprint={}, existingFingerprint={}",
                            userId, subscription.getIndicatorCode(), barTime,
                            fingerprint, existingValue.getCalcFingerprint());
                    
                    return true; // 检测到冲突
                }
            }
            
            return false; // 未检测到冲突
            
        } catch (Exception e) {
            log.error("冲突检测失败: userId={}, indicatorCode={}",
                    userId, subscription.getIndicatorCode(), e);
            return false;
        }
    }
}

