package com.qyl.v2trade.indicator.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 评估请求项DTO（V2新增）
 * 
 * <p>用于批量评估请求中的单个评估项
 *
 * @author qyl
 */
@Data
public class EvaluationRequestItemDTO {
    
    @NotBlank(message = "indicatorCode不能为空")
    private String indicatorCode;
    
    @NotBlank(message = "indicatorVersion不能为空")
    private String indicatorVersion;
    
    private Map<String, Object> params;
}

