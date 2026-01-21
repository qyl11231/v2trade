package com.qyl.v2trade.indicator.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 评估请求DTO（V2新增）
 * 
 * <p>用于单次评估接口
 *
 * @author qyl
 */
@Data
public class EvaluateRequestDTO {
    
    @NotNull(message = "userId不能为空")
    private Long userId;
    
    @NotNull(message = "tradingPairId不能为空")
    private Long tradingPairId;
    
    @NotBlank(message = "timeframe不能为空")
    private String timeframe;
    
    @NotNull(message = "asOfBarTime不能为空")
    private LocalDateTime asOfBarTime;  // bar_close_time UTC
    
    @NotBlank(message = "indicatorCode不能为空")
    private String indicatorCode;
    
    @NotBlank(message = "indicatorVersion不能为空")
    private String indicatorVersion;
    
    private Map<String, Object> params;
}

