package com.qyl.v2trade.indicator.runtime;

import com.qyl.v2trade.indicator.calculator.IndicatorEngineRouter;
import com.qyl.v2trade.indicator.cache.IndicatorCacheManager;
import com.qyl.v2trade.indicator.engine.IndicatorComputeRequest;
import com.qyl.v2trade.indicator.engine.IndicatorEngine;
import com.qyl.v2trade.indicator.engine.IndicatorResult;
import com.qyl.v2trade.indicator.observability.IndicatorMetrics;
import com.qyl.v2trade.indicator.persistence.CalcFingerprint;
import com.qyl.v2trade.indicator.repository.IndicatorDefinitionRepository;
import com.qyl.v2trade.indicator.repository.entity.IndicatorDefinition;
import com.qyl.v2trade.indicator.series.BarSeriesManager;
import com.qyl.v2trade.indicator.series.BarSeriesView;
import com.qyl.v2trade.indicator.validation.IndicatorParamValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 指标评估服务（V2核心服务）
 * 
 * <p>【🔴 职责边界（必须遵守）】
 * - IndicatorEvaluateService 是"纯运行时评估服务"，只负责协调调用
 * - 具体逻辑必须拆分到子组件：
 *   - `IndicatorParamValidator`：参数校验
 *   - `IndicatorEngineRouter`：引擎路由
 *   - `IndicatorCacheManager`：缓存管理（阶段三可先空实现）
 * - **严禁**：将业务逻辑直接写在 IndicatorEvaluateService 中
 * 
 * <p>【核心能力】
 * - 提供统一的按需评估入口（evaluate/evaluateBatch）
 * - 支持单次评估和批量评估
 * - 支持缓存命中（阶段四完善）
 * 
 * <p>【输出边界】
 * - 只返回计算结果，不返回策略语义
 * - 允许：values、valid、fingerprint、source、costMs、errorMsg
 * - 禁止：tradeAction、positionSide、signalScore 等策略语义
 *
 * @author qyl
 */
@Slf4j
@Service
public class IndicatorEvaluateService {
    
    @Autowired
    private IndicatorParamValidator paramValidator;
    
    @Autowired
    private IndicatorEngineRouter engineRouter;
    
    @Autowired
    private IndicatorCacheManager cacheManager;
    
    @Autowired
    private IndicatorDefinitionRepository definitionRepository;
    
    @Autowired
    private BarSeriesManager barSeriesManager;
    
    @Autowired(required = false)
    private IndicatorMetrics metrics;
    
    /**
     * 单次评估
     * 
     * <p>【职责边界】只做协调调用，具体逻辑在子组件
     * 
     * @param indicatorCode 指标编码
     * @param version 版本
     * @param params 参数
     * @param context 评估上下文（pairId、timeframe、asOfBarTime）
     * @return 评估结果
     */
    public IndicatorEvaluateResult evaluate(
            String indicatorCode,
            String version,
            Map<String, Object> params,
            EvaluationContext context) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 参数校验（调用 IndicatorParamValidator）
            paramValidator.validate(indicatorCode, version, params);
            
            // 2. 生成缓存键（调用 IndicatorCacheManager，阶段三可先空实现）
            String cacheKey = cacheManager.generateCacheKey(indicatorCode, version, context, params);
            
            // 3. 查询缓存（调用 IndicatorCacheManager）
            Optional<IndicatorEvaluateResult> cached = cacheManager.get(cacheKey);
            if (cached.isPresent()) {
                IndicatorEvaluateResult cachedResult = cached.get();
                // 确保 source 字段为 CACHE
                if (!"CACHE".equals(cachedResult.getSource())) {
                    cachedResult = IndicatorEvaluateResult.builder()
                            .valid(cachedResult.isValid())
                            .source("CACHE")
                            .values(cachedResult.getValues())
                            .fingerprint(cachedResult.getFingerprint())
                            .costMs(cachedResult.getCostMs())
                            .errorMsg(cachedResult.getErrorMsg())
                            .build();
                }
                log.debug("缓存命中: code={}, version={}, pairId={}, timeframe={}", 
                        indicatorCode, version, context.getTradingPairId(), context.getTimeframe());
                
                // 记录Metrics：缓存命中
                if (metrics != null) {
                    metrics.recordCacheHit(indicatorCode);
                    int costMs = (int) (System.currentTimeMillis() - startTime);
                    metrics.recordEvaluateCost(costMs, indicatorCode);
                }
                
                return cachedResult;
            }
            
            // 记录Metrics：缓存未命中
            if (metrics != null) {
                metrics.recordCacheMiss(indicatorCode);
            }
            
            // 4. 缓存未命中：执行计算
            //    - 获取指标定义（Repository）
            IndicatorDefinition definition = definitionRepository.findByCodeAndVersion(
                    indicatorCode, version)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "指标定义不存在: code=" + indicatorCode + ", version=" + version));
            
            //    - 获取 BarSeries（BarSeriesManager）
            BarSeriesView series = barSeriesManager.getSeries(
                    context.getTradingPairId(), context.getTimeframe());
            
            if (series == null) {
                return IndicatorEvaluateResult.invalid(
                        "BarSeries不存在: pairId=" + context.getTradingPairId() + 
                        ", timeframe=" + context.getTimeframe());
            }
            
            //    - 检查 min_required_bars
            int barCount = series.size();
            int minRequiredBars = definition.getMinRequiredBars() != null ? definition.getMinRequiredBars() : 1;
            if (barCount < minRequiredBars) {
                return IndicatorEvaluateResult.invalid(
                        "Bar数量不足: " + barCount + " < " + minRequiredBars);
            }
            
            //    - 路由到对应引擎（调用 IndicatorEngineRouter）
            IndicatorEngine engine = engineRouter.getEngine(definition);
            if (engine == null) {
                return IndicatorEvaluateResult.invalid(
                        "找不到引擎: implKey=" + definition.getImplKey() + 
                        ", engine=" + definition.getEngine());
            }
            
            //    - 对齐 asOfBarTime 到该时间周期的 Bar 开始时间
            //      例如：5分钟线，2026-01-21T01:07:59 -> 2026-01-21T01:05:00
            LocalDateTime alignedBarTime = alignToBarStartTime(
                    context.getAsOfBarTime(), context.getTimeframe());
            
            //    - 构建计算请求
            IndicatorComputeRequest request = new IndicatorComputeRequest(
                    indicatorCode,
                    version,
                    params != null ? params : new HashMap<>(),
                    context.getTradingPairId(),
                    context.getTimeframe(),
                    alignedBarTime
            );
            
            //    - 执行计算
            IndicatorResult result = engine.compute(request, series);
            
            //    - 校验返回结果（return_schema）
            validateReturnSchema(result, definition);
            
            //    - 生成计算指纹
            String engineName = engine.getEngineName();
            String fingerprint = CalcFingerprint.generate(
                    indicatorCode, version, params != null ? params : new HashMap<>(), engineName);
            
            //    - 构建 IndicatorEvaluateResult
            int costMs = (int) (System.currentTimeMillis() - startTime);
            IndicatorEvaluateResult evaluateResult;
            
            if (result.status() == IndicatorResult.Status.SUCCESS) {
                evaluateResult = IndicatorEvaluateResult.success(
                        result.values(),
                        fingerprint,
                        costMs,
                        "COMPUTED"
                );
            } else {
                evaluateResult = IndicatorEvaluateResult.invalid(
                        result.errorMessage() != null ? result.errorMessage() : "计算失败");
                evaluateResult.setCostMs(costMs);
            }
            
            //    - 写入缓存（调用 IndicatorCacheManager）
            cacheManager.put(cacheKey, evaluateResult);
            
            // 5. 记录Metrics：评估耗时
            if (metrics != null) {
                metrics.recordEvaluateCost(costMs, indicatorCode);
            }
            
            // 6. 返回结果
            return evaluateResult;
            
        } catch (IndicatorParamValidator.ValidationException e) {
            // 参数校验失败
            int costMs = (int) (System.currentTimeMillis() - startTime);
            
            // 记录Metrics：评估失败
            if (metrics != null) {
                metrics.recordEvaluateFailure(indicatorCode);
                metrics.recordEvaluateCost(costMs, indicatorCode);
            }
            
            IndicatorEvaluateResult result = IndicatorEvaluateResult.builder()
                    .valid(false)
                    .source("COMPUTED")
                    .values(null)
                    .fingerprint(null)
                    .costMs(costMs)
                    .errorMsg(e.getMessage())
                    .build();
            return result;
            
        } catch (Exception e) {
            // 其他异常
            log.error("指标评估失败: code={}, version={}, pairId={}, timeframe={}", 
                    indicatorCode, version, context.getTradingPairId(), context.getTimeframe(), e);
            int costMs = (int) (System.currentTimeMillis() - startTime);
            
            // 记录Metrics：评估失败
            if (metrics != null) {
                metrics.recordEvaluateFailure(indicatorCode);
                metrics.recordEvaluateCost(costMs, indicatorCode);
            }
            
            return IndicatorEvaluateResult.builder()
                    .valid(false)
                    .source("COMPUTED")
                    .values(null)
                    .fingerprint(null)
                    .costMs(costMs)
                    .errorMsg("评估异常: " + e.getMessage())
                    .build();
        }
    }
    
    /**
     * 批量评估
     * 
     * <p>【职责边界】只做协调调用，具体逻辑在子组件
     * 
     * @param context 评估上下文
     * @param requests 评估请求列表
     * @return 评估结果列表（与 requests 顺序一致）
     */
    public List<IndicatorEvaluateResult> evaluateBatch(
            EvaluationContext context,
            List<EvaluationRequest> requests) {
        
        if (requests == null || requests.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 记录Metrics：批量评估大小
        if (metrics != null) {
            metrics.recordBatchSize(requests.size());
        }
        
        // 1. 批量生成缓存键
        List<String> cacheKeys = new ArrayList<>();
        for (EvaluationRequest request : requests) {
            String cacheKey = cacheManager.generateCacheKey(
                    request.getIndicatorCode(), 
                    request.getIndicatorVersion(), 
                    context, 
                    request.getParams());
            cacheKeys.add(cacheKey);
        }
        
        // 2. 批量查询缓存
        Map<String, IndicatorEvaluateResult> cachedResults = new HashMap<>();
        for (int i = 0; i < cacheKeys.size(); i++) {
            String cacheKey = cacheKeys.get(i);
            Optional<IndicatorEvaluateResult> cached = cacheManager.get(cacheKey);
            if (cached.isPresent()) {
                cachedResults.put(cacheKey, cached.get());
            }
        }
        
        // 3. 对于缓存未命中的请求，批量执行计算
        // 注意：批量评估时，每个请求可能使用不同的 pairId/timeframe，所以不能提前获取 BarSeries
        // BarSeries 的获取在 evaluate() 方法内部进行，但批量评估可以复用相同的 context
        
        List<IndicatorEvaluateResult> results = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            String cacheKey = cacheKeys.get(i);
            EvaluationRequest request = requests.get(i);
            
            // 如果缓存命中，直接使用
            if (cachedResults.containsKey(cacheKey)) {
                results.add(cachedResults.get(cacheKey));
                continue;
            }
            
            // 缓存未命中：执行计算
            IndicatorEvaluateResult result = evaluate(
                    request.getIndicatorCode(),
                    request.getIndicatorVersion(),
                    request.getParams(),
                    context);
            results.add(result);
        }
        
        // 4. 返回结果列表（与 requests 顺序一致）
        return results;
    }
    
    /**
     * 校验返回结果（基于 return_schema）
     * 
     * <p>【职责边界】这是 Service 内部的辅助方法，用于结果校验
     * 
     * @param result 计算结果
     * @param definition 指标定义
     */
    private void validateReturnSchema(IndicatorResult result, IndicatorDefinition definition) {
        // 阶段三：先简单校验，后续可以基于 return_schema 进行更详细的校验
        if (result.status() == IndicatorResult.Status.SUCCESS) {
            if (result.values() == null || result.values().isEmpty()) {
                log.warn("计算结果为空: code={}, version={}", 
                        definition.getIndicatorCode(), definition.getIndicatorVersion());
            }
        }
        
        // TODO: 后续可以基于 return_schema 进行更详细的校验
        // 例如：检查返回值的键是否符合 return_schema 的定义
    }
    
    /**
     * 将时间对齐到该时间周期的 Bar 收盘时间（bar_close_time）
     * 
     * <p>【重要】asOfBarTime 是 bar_close_time 语义，需要向下对齐到上一个已经收盘的 Bar
     * <p>【原则】适用于所有时间周期（5m, 15m, 30m, 1h, 4h 等），都使用相同的对齐规则
     * 
     * <p>对齐规则：
     * <ul>
     *   <li>如果原始时间正好是某个Bar的收盘时间（且秒为0），保持不变</li>
     *   <li>否则，向下对齐到上一个已经收盘的Bar的收盘时间</li>
     * </ul>
     * 
     * <p>示例（所有周期都遵循此规则）：
     * <ul>
     *   <li><b>5分钟线</b>：
     *     <ul>
     *       <li>01:07:59 -> 01:05:00（向下对齐到已收盘的Bar）</li>
     *       <li>01:05:00 -> 01:05:00（正好是收盘时间，保持不变）</li>
     *       <li>01:12:00 -> 01:10:00（向下对齐到已收盘的Bar）</li>
     *     </ul>
     *   </li>
     *   <li><b>15分钟线</b>：
     *     <ul>
     *       <li>01:07:59 -> 01:00:00（向下对齐到已收盘的Bar）</li>
     *       <li>01:15:00 -> 01:15:00（正好是收盘时间，保持不变）</li>
     *       <li>01:20:00 -> 01:15:00（向下对齐到已收盘的Bar）</li>
     *     </ul>
     *   </li>
     *   <li><b>30分钟线</b>：
     *     <ul>
     *       <li>01:07:59 -> 01:00:00（向下对齐到已收盘的Bar）</li>
     *       <li>01:30:00 -> 01:30:00（正好是收盘时间，保持不变）</li>
     *       <li>01:45:00 -> 01:30:00（向下对齐到已收盘的Bar）</li>
     *     </ul>
     *   </li>
     *   <li><b>1小时线</b>：
     *     <ul>
     *       <li>01:07:59 -> 01:00:00（向下对齐到已收盘的Bar）</li>
     *       <li>02:00:00 -> 02:00:00（正好是收盘时间，保持不变）</li>
     *       <li>02:30:00 -> 02:00:00（向下对齐到已收盘的Bar）</li>
     *     </ul>
     *   </li>
     *   <li><b>4小时线</b>：
     *     <ul>
     *       <li>01:07:59 -> 00:00:00（向下对齐到已收盘的Bar）</li>
     *       <li>04:00:00 -> 04:00:00（正好是收盘时间，保持不变）</li>
     *       <li>05:30:00 -> 04:00:00（向下对齐到已收盘的Bar）</li>
     *     </ul>
     *   </li>
     * </ul>
     * 
     * @param time 原始时间
     * @param timeframe 时间周期（如 "5m", "15m", "30m", "1h", "4h"）
     * @return 对齐后的 Bar 收盘时间（已收盘的最后一个Bar）
     */
    private LocalDateTime alignToBarStartTime(LocalDateTime time, String timeframe) {
        if (time == null || timeframe == null || timeframe.isEmpty()) {
            return time;
        }
        
        try {
            // 解析 timeframe
            String numberStr = timeframe.replaceAll("[^0-9]", "");
            String unit = timeframe.replaceAll("[0-9]", "").toLowerCase();
            
            if (numberStr.isEmpty()) {
                // 默认1分钟，向下对齐
                return time.truncatedTo(ChronoUnit.MINUTES);
            }
            
            long number = Long.parseLong(numberStr);
            LocalDateTime aligned = time;
            
            // 根据单位对齐时间（向下对齐到已收盘的Bar）
            switch (unit) {
                case "m": // 分钟
                    long minutes = time.getMinute();
                    long seconds = time.getSecond();
                    // 向下对齐到周期的开始
                    long alignedMinutesStart = (minutes / number) * number;
                    LocalDateTime barStart = time.withMinute((int) alignedMinutesStart).withSecond(0).withNano(0);
                    
                    // 如果原始时间正好是周期的开始（且秒为0），说明就是收盘时间，保持不变
                    if (minutes == alignedMinutesStart && seconds == 0) {
                        aligned = barStart;
                    } else {
                        // 否则，应该找上一个已经收盘的Bar，也就是 barStart（当前周期的收盘时间）
                        // 例如：01:07:59 -> 向下对齐到 01:00:00（上一个已收盘的Bar）
                        aligned = barStart;
                    }
                    break;
                    
                case "h": // 小时
                    long hours = time.getHour();
                    long mins = time.getMinute();
                    long secs = time.getSecond();
                    // 向下对齐到周期的开始
                    long alignedHoursStart = (hours / number) * number;
                    LocalDateTime barStartHour = time.withHour((int) alignedHoursStart).withMinute(0).withSecond(0).withNano(0);
                    
                    // 如果原始时间正好是周期的开始（且分钟和秒为0），说明就是收盘时间，保持不变
                    if (hours == alignedHoursStart && mins == 0 && secs == 0) {
                        aligned = barStartHour;
                    } else {
                        // 否则，应该找上一个已经收盘的Bar
                        aligned = barStartHour;
                    }
                    break;
                    
                case "d": // 天
                    long dayOfYear = time.getDayOfYear();
                    long alignedDayStart;
                    if (number > 1) {
                        alignedDayStart = ((dayOfYear - 1) / number) * number + 1;
                    } else {
                        alignedDayStart = dayOfYear;
                    }
                    aligned = time.withDayOfYear((int) alignedDayStart).withHour(0).withMinute(0).withSecond(0).withNano(0);
                    // 如果原始时间正好是周期的开始，保持不变
                    if (dayOfYear != alignedDayStart || time.getHour() != 0 || time.getMinute() != 0 || time.getSecond() != 0) {
                        // 否则，已经是上一个已收盘的Bar
                    }
                    break;
                    
                default:
                    log.warn("未知的timeframe单位: {}, 使用默认对齐（分钟）", unit);
                    aligned = time.truncatedTo(ChronoUnit.MINUTES);
            }
            
            log.debug("时间对齐（已收盘的Bar收盘时间）: {} (timeframe={}) -> {}", time, timeframe, aligned);
            return aligned;
            
        } catch (Exception e) {
            log.warn("对齐时间失败: time={}, timeframe={}, error={}", time, timeframe, e.getMessage());
            // 失败时返回原始时间，让引擎自己去处理
            return time;
        }
    }
}

