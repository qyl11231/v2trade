package com.qyl.v2trade.indicator.api;

import com.qyl.v2trade.common.Result;
import com.qyl.v2trade.indicator.api.dto.*;
import com.qyl.v2trade.indicator.runtime.EvaluationContext;
import com.qyl.v2trade.indicator.runtime.EvaluationRequest;
import com.qyl.v2trade.indicator.runtime.IndicatorEvaluateResult;
import com.qyl.v2trade.indicator.runtime.IndicatorEvaluateService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 指标评估API控制器（V2新增）
 * 
 * <p>提供指标评估的REST接口，供策略模块、回测模块、前端页面调用
 * 
 * <p>【核心接口】
 * - POST /api/indicator/evaluate：单次评估
 * - POST /api/indicator/evaluate/batch：批量评估
 *
 * @author qyl
 */
@Slf4j
@RestController
@RequestMapping("/api/indicator/evaluate")
public class IndicatorEvaluateController {
    
    @Autowired
    private IndicatorEvaluateService evaluateService;
    
    /**
     * 单次评估
     * 
     * <p>POST /api/indicator/evaluate
     * 
     * <p>【请求示例】
     * ```json
     * {
     *   "userId": 1,
     *   "tradingPairId": 1,
     *   "timeframe": "1h",
     *   "asOfBarTime": "2024-01-01T12:00:00",
     *   "indicatorCode": "RSI",
     *   "indicatorVersion": "v1",
     *   "params": {"period": 14}
     * }
     * ```
     * 
     * <p>【响应示例】
     * ```json
     * {
     *   "code": 200,
     *   "message": "操作成功",
     *   "data": {
     *     "valid": true,
     *     "source": "CACHE",
     *     "values": {"value": 65.5},
     *     "fingerprint": "abc123...",
     *     "costMs": 1
     *   }
     * }
     * ```
     */
    @PostMapping
    public Result<IndicatorEvaluateResultDTO> evaluate(
            @RequestBody @Valid EvaluateRequestDTO request) {
        
        try {
            // 1. 构建 EvaluationContext
            EvaluationContext context = new EvaluationContext(
                    request.getUserId(),
                    request.getTradingPairId(),
                    request.getTimeframe(),
                    request.getAsOfBarTime()
            );
            
            // 2. 调用 evaluateService.evaluate()
            IndicatorEvaluateResult result = evaluateService.evaluate(
                    request.getIndicatorCode(),
                    request.getIndicatorVersion(),
                    request.getParams(),
                    context
            );
            
            // 3. 转换为 DTO 返回
            IndicatorEvaluateResultDTO dto = IndicatorEvaluateResultDTO.fromResult(result);
            return Result.success(dto);
            
        } catch (Exception e) {
            log.error("指标评估失败: request={}", request, e);
            return Result.error("评估失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量评估
     * 
     * <p>POST /api/indicator/evaluate/batch
     * 
     * <p>【请求示例】
     * ```json
     * {
     *   "userId": 1,
     *   "tradingPairId": 1,
     *   "timeframe": "1h",
     *   "asOfBarTime": "2024-01-01T12:00:00",
     *   "requests": [
     *     {"indicatorCode": "RSI", "indicatorVersion": "v1", "params": {"period": 14}},
     *     {"indicatorCode": "MACD", "indicatorVersion": "v1", "params": {"fast": 12, "slow": 26}}
     *   ]
     * }
     * ```
     * 
     * <p>【响应示例】
     * ```json
     * {
     *   "code": 200,
     *   "message": "操作成功",
     *   "data": [
     *     {"valid": true, "source": "CACHE", "values": {...}},
     *     {"valid": true, "source": "COMPUTED", "values": {...}}
     *   ]
     * }
     * ```
     */
    @PostMapping("/batch")
    public Result<List<IndicatorEvaluateResultDTO>> evaluateBatch(
            @RequestBody @Valid BatchEvaluateRequestDTO request) {
        
        try {
            // 1. 构建 EvaluationContext
            EvaluationContext context = new EvaluationContext(
                    request.getUserId(),
                    request.getTradingPairId(),
                    request.getTimeframe(),
                    request.getAsOfBarTime()
            );
            
            // 2. 构建 EvaluationRequest 列表
            List<EvaluationRequest> requests = request.getRequests().stream()
                    .map(item -> new EvaluationRequest(
                            item.getIndicatorCode(),
                            item.getIndicatorVersion(),
                            item.getParams()
                    ))
                    .collect(Collectors.toList());
            
            // 3. 调用 evaluateService.evaluateBatch()
            List<IndicatorEvaluateResult> results = evaluateService.evaluateBatch(context, requests);
            
            // 4. 转换为 DTO 列表返回
            List<IndicatorEvaluateResultDTO> dtos = results.stream()
                    .map(IndicatorEvaluateResultDTO::fromResult)
                    .collect(Collectors.toList());
            
            return Result.success(dtos);
            
        } catch (Exception e) {
            log.error("批量评估失败: request={}", request, e);
            return Result.error("批量评估失败: " + e.getMessage());
        }
    }
}

