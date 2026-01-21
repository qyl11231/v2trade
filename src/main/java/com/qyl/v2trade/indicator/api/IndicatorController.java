package com.qyl.v2trade.indicator.api;

import com.qyl.v2trade.common.Result;
import com.qyl.v2trade.indicator.runtime.EvaluationContext;
import com.qyl.v2trade.indicator.runtime.IndicatorEvaluateResult;
import com.qyl.v2trade.indicator.runtime.IndicatorEvaluateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 指标查询API（V2：向后兼容）
 * 
 * <p>提供指标值的查询接口，供策略模块使用
 * 
 * <p>【V2 变化】
 * - 保留接口路径和参数不变（向后兼容）
 * - 查询逻辑改为：优先从数据库查（白名单指标），否则按需计算
 * - 响应里增加 `source` 字段（CACHE/COMPUTED/DB）
 *
 * @author qyl
 */
@Slf4j
@RestController
@RequestMapping("/api/indicator")
public class IndicatorController {
    
    @Autowired
    private IndicatorEvaluateService evaluateService;
    
    /**
     * 查询最新指标值（V2：向后兼容）
     * 
     * <p>GET /api/indicator/latest?userId=&pairId=&timeframe=&code=&version=
     * 
     * <p>【V2 查询逻辑】
     * 1. 先尝试从数据库查询（如果有白名单指标的历史数据）
     * 2. 否则：调用 `evaluateService.evaluate()` 按需计算
     * 
     * <p>【响应格式】
     * - 如果从数据库查：`source=DB`
     * - 如果按需计算且缓存命中：`source=CACHE`
     * - 如果按需计算且缓存未命中：`source=COMPUTED`
     * 
     * @param userId 用户ID（必填）
     * @param pairId 交易对ID（必填）
     * @param timeframe 周期（必填）
     * @param code 指标编码（必填）
     * @param version 指标版本（可选，默认v1）
     * @return 最新指标值，不存在返回null
     */
    @GetMapping("/latest")
    public Result<Map<String, Object>> getLatest(
            @RequestParam("userId") Long userId,
            @RequestParam("pairId") Long pairId,
            @RequestParam("timeframe") String timeframe,
            @RequestParam("code") String code,
            @RequestParam(value = "version", required = false, defaultValue = "v1") String version) {
        
        try {
            // 参数校验
            if (userId == null || pairId == null || timeframe == null || code == null) {
                return Result.error(400, "参数不完整：userId、pairId、timeframe、code为必填项");
            }

            // V2: 查询逻辑改为按需评估
            // 1. 先尝试从数据库查询（如果有白名单指标的历史数据）
            //    注意：IndicatorValueRepository 已废弃，V2 不再从数据库查询
            //    如果需要保留数据库查询功能，需要重新实现或使用其他方式
            
            // 2. 按需计算（使用当前时间作为 asOfBarTime）
            EvaluationContext context = new EvaluationContext(
                    userId, 
                    pairId, 
                    timeframe, 
                    LocalDateTime.now()  // 使用当前时间
            );
            
            IndicatorEvaluateResult result = evaluateService.evaluate(
                    code, 
                    version, 
                    new HashMap<>(),  // 默认无参数
                    context
            );
            
            if (!result.isValid()) {
                // 不存在或无效，返回 null
                return Result.success(null);
            }
            
            // 转换为兼容格式的响应
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("tradingPairId", pairId);
            response.put("timeframe", timeframe);
            response.put("indicatorCode", code);
            response.put("indicatorVersion", version);
            response.put("barTime", context.getAsOfBarTime());
            response.put("value", result.getSingleValue());  // 单值指标
            response.put("extraValues", result.getValues());  // 多值指标
            response.put("source", result.getSource());  // CACHE / COMPUTED
            response.put("calcCostMs", result.getCostMs());
            response.put("fingerprint", result.getFingerprint());
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("查询最新指标值失败: userId={}, pairId={}, timeframe={}, code={}, version={}",
                    userId, pairId, timeframe, code, version, e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
}

