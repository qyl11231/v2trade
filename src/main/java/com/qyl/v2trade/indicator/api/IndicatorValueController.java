package com.qyl.v2trade.indicator.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qyl.v2trade.common.Result;
import com.qyl.v2trade.indicator.api.dto.IndicatorValueDTO;
import com.qyl.v2trade.indicator.repository.IndicatorValueRepository;
import com.qyl.v2trade.indicator.repository.entity.IndicatorValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 指标结果API控制器
 * 
 * <p>提供指标值的查询接口（只读）
 *
 * @author qyl
 */
@Slf4j
@RestController
@RequestMapping("/api/indicator/values")
public class IndicatorValueController {
    
    @Autowired
    private IndicatorValueRepository valueRepository;
    
    /**
     * 查询最新指标值
     * 
     * <p>GET /api/indicator/values/latest?userId=&tradingPairId=&timeframe=&indicatorCode=&indicatorVersion=
     */
    @GetMapping("/latest")
    public Result<IndicatorValueDTO> getLatest(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long tradingPairId,
            @RequestParam(required = false) String timeframe,
            @RequestParam(required = false) String indicatorCode,
            @RequestParam(required = false, defaultValue = "v1") String indicatorVersion) {
        
        try {
            log.debug("查询最新指标值: userId={}, tradingPairId={}, timeframe={}, indicatorCode={}, version={}",
                    userId, tradingPairId, timeframe, indicatorCode, indicatorVersion);
            
            if (userId == null || tradingPairId == null || timeframe == null || indicatorCode == null) {
                return Result.error(400, "参数不完整：userId、tradingPairId、timeframe、indicatorCode为必填项");
            }
            
            IndicatorValue value = valueRepository.findLatest(
                    userId, tradingPairId, timeframe, indicatorCode, indicatorVersion)
                    .orElse(null);
            
            if (value == null) {
                return Result.error(404, "未找到指标值");
            }
            
            return Result.success(IndicatorValueDTO.fromEntity(value));
            
        } catch (Exception e) {
            log.error("查询最新指标值失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
    
    /**
     * 查询指标值历史（分页）
     * 
     * <p>GET /api/indicator/values?userId=&tradingPairId=&timeframe=&indicatorCode=&indicatorVersion=&startTime=&endTime=&page=1&size=200
     */
    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long tradingPairId,
            @RequestParam(required = false) String timeframe,
            @RequestParam(required = false) String indicatorCode,
            @RequestParam(required = false) String indicatorVersion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "200") int size) {
        
        try {
            log.debug("查询指标值历史: userId={}, tradingPairId={}, timeframe={}, indicatorCode={}, page={}, size={}",
                    userId, tradingPairId, timeframe, indicatorCode, page, size);
            
            Page<IndicatorValue> pageResult = valueRepository.queryWithPagination(
                    userId, tradingPairId, timeframe, indicatorCode, indicatorVersion,
                    startTime, endTime, page, size);
            
            List<IndicatorValueDTO> dtos = pageResult.getRecords().stream()
                    .map(IndicatorValueDTO::fromEntity)
                    .collect(Collectors.toList());
            
            Map<String, Object> data = new HashMap<>();
            data.put("records", dtos);
            data.put("total", pageResult.getTotal());
            data.put("current", pageResult.getCurrent());
            data.put("size", pageResult.getSize());
            
            return Result.success(data);
            
        } catch (Exception e) {
            log.error("查询指标值历史失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
}

