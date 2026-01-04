package com.qyl.v2trade.market.aggregation.web;

import com.qyl.v2trade.common.Result;
import com.qyl.v2trade.market.aggregation.core.AggregationMetrics;
import com.qyl.v2trade.market.aggregation.core.AggregationStats;
import com.qyl.v2trade.market.aggregation.core.KlineAggregator;
import com.qyl.v2trade.market.aggregation.core.impl.KlineAggregatorImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 聚合监控控制器
 * 
 * <p>提供聚合模块的监控指标接口
 *
 * @author qyl
 */
@Slf4j
@RestController
@RequestMapping("/api/market/aggregation")
public class AggregationMonitorController {
    
    @Autowired
    private KlineAggregator klineAggregator;
    
    /**
     * 获取聚合统计信息
     */
    @GetMapping("/stats")
    public Result<AggregationStats> getStats() {
        try {
            AggregationStats stats = klineAggregator.getStats();
            return Result.success("获取成功", stats);
        } catch (Exception e) {
            log.error("获取聚合统计信息失败", e);
            return Result.error("获取失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取聚合监控指标
     */
    @GetMapping("/metrics")
    public Result<Map<String, Object>> getMetrics() {
        try {
            if (klineAggregator instanceof KlineAggregatorImpl) {
                AggregationMetrics metrics = ((KlineAggregatorImpl) klineAggregator).getMetrics();
                
                Map<String, Object> result = new HashMap<>();
                result.put("totalEventCount", metrics.getTotalEventCount());
                result.put("totalAggregatedCount", metrics.getTotalAggregatedCount());
                result.put("successCount", metrics.getSuccessCount());
                result.put("failCount", metrics.getFailCount());
                result.put("successRate", metrics.getSuccessRate());
                result.put("writeSuccessCount", metrics.getWriteSuccessCount());
                result.put("writeFailCount", metrics.getWriteFailCount());
                result.put("writeSkipCount", metrics.getWriteSkipCount());
                result.put("writeSuccessRate", metrics.getWriteSuccessRate());
                result.put("outOfOrderRejectCount", metrics.getOutOfOrderRejectCount());
                result.put("duplicateIgnoreCount", metrics.getDuplicateIgnoreCount());
                result.put("averageAggregationLatencyMs", metrics.getAverageAggregationLatencyMs());
                result.put("maxAggregationLatencyMs", metrics.getMaxAggregationLatencyMs());
                result.put("minAggregationLatencyMs", metrics.getMinAggregationLatencyMs());
                
                // 告警检查
                Map<String, Boolean> alerts = new HashMap<>();
                alerts.put("highLatency", metrics.getMaxAggregationLatencyMs() > 10.0);
                alerts.put("highWriteFailRate", metrics.getWriteSuccessRate() < 99.0);
                result.put("alerts", alerts);
                
                return Result.success("获取成功", result);
            } else {
                return Result.error("监控指标不可用");
            }
        } catch (Exception e) {
            log.error("获取聚合监控指标失败", e);
            return Result.error("获取失败: " + e.getMessage());
        }
    }
}

