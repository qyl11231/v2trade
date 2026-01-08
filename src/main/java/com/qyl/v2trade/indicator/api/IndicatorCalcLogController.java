package com.qyl.v2trade.indicator.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qyl.v2trade.common.Result;
import com.qyl.v2trade.indicator.repository.IndicatorCalcLogRepository;
import com.qyl.v2trade.indicator.repository.entity.IndicatorCalcLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 指标计算日志API控制器
 * 
 * <p>提供计算日志的查询接口（只读）
 *
 * @author qyl
 */
@Slf4j
@RestController
@RequestMapping("/api/indicator/calc-logs")
public class IndicatorCalcLogController {
    
    @Autowired
    private IndicatorCalcLogRepository calcLogRepository;
    
    /**
     * 查询计算日志（分页）
     * 
     * <p>GET /api/indicator/calc-logs?userId=&tradingPairId=&timeframe=&indicatorCode=&status=&startTime=&endTime=&page=1&size=200
     */
    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long tradingPairId,
            @RequestParam(required = false) String timeframe,
            @RequestParam(required = false) String indicatorCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "200") int size) {
        
        try {
            log.debug("查询计算日志: userId={}, tradingPairId={}, timeframe={}, indicatorCode={}, status={}, page={}, size={}",
                    userId, tradingPairId, timeframe, indicatorCode, status, page, size);
            
            Page<IndicatorCalcLog> pageResult = calcLogRepository.queryWithPagination(
                    userId, tradingPairId, timeframe, indicatorCode, status,
                    startTime, endTime, page, size);
            
            Map<String, Object> data = new HashMap<>();
            data.put("records", pageResult.getRecords());
            data.put("total", pageResult.getTotal());
            data.put("current", pageResult.getCurrent());
            data.put("size", pageResult.getSize());
            
            return Result.success(data);
            
        } catch (Exception e) {
            log.error("查询计算日志失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
}

