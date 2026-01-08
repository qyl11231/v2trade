package com.qyl.v2trade.indicator.api;

import com.qyl.v2trade.common.Result;
import com.qyl.v2trade.indicator.api.dto.IndicatorValueDTO;
import com.qyl.v2trade.indicator.repository.IndicatorValueRepository;
import com.qyl.v2trade.indicator.repository.entity.IndicatorValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * 指标查询API（只读）
 * 
 * <p>提供指标值的查询接口，供策略模块使用
 * 
 * <p>特点：
 * - 只读，不触发计算
 * - 从MySQL查询已计算好的指标值
 *
 * @author qyl
 */
@Slf4j
@RestController
@RequestMapping("/api/indicator")
public class IndicatorController {
    
    @Autowired
    private IndicatorValueRepository valueRepository;
    
    /**
     * 查询最新指标值
     * 
     * <p>GET /api/indicator/latest?userId=&pairId=&timeframe=&code=&version=
     * 
     * <p>从MySQL indicator_value表查询最新一条（按bar_time desc limit 1）
     * 
     * @param userId 用户ID（必填）
     * @param pairId 交易对ID（必填）
     * @param timeframe 周期（必填）
     * @param code 指标编码（必填）
     * @param version 指标版本（可选，默认v1）
     * @return 最新指标值，不存在返回null
     */
    @GetMapping("/latest")
    public Result<IndicatorValueDTO> getLatest(
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
            
            // 查询最新指标值
            Optional<IndicatorValue> valueOpt = valueRepository.findLatest(userId, pairId, timeframe, code, version);
            
            if (valueOpt.isEmpty()) {
                log.debug("未找到指标值: userId={}, pairId={}, timeframe={}, code={}, version={}",
                        userId, pairId, timeframe, code, version);
                return Result.success(null);
            }
            
            // 转换为DTO
            IndicatorValueDTO dto = IndicatorValueDTO.fromEntity(valueOpt.get());
            
            log.debug("查询到最新指标值: userId={}, pairId={}, timeframe={}, code={}, version={}, barTime={}",
                    userId, pairId, timeframe, code, version, dto.barTime());
            
            return Result.success(dto);
            
        } catch (Exception e) {
            log.error("查询最新指标值失败: userId={}, pairId={}, timeframe={}, code={}, version={}",
                    userId, pairId, timeframe, code, version, e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
}

