package com.qyl.v2trade.indicator.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 批量评估请求DTO（V2新增）
 * 
 * <p>用于批量评估接口
 *
 * @author qyl
 */
@Data
public class BatchEvaluateRequestDTO {
    
    @NotNull(message = "userId不能为空")
    private Long userId;
    
    @NotNull(message = "tradingPairId不能为空")
    private Long tradingPairId;
    
    @NotBlank(message = "timeframe不能为空")
    private String timeframe;
    
    @NotNull(message = "asOfBarTime不能为空")
    private LocalDateTime asOfBarTime;
    
    @NotEmpty(message = "requests不能为空")
    private List<EvaluationRequestItemDTO> requests;  // 评估请求列表
}

