package com.qyl.v2trade.indicator.runtime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 评估请求（V2新增）
 * 
 * <p>用于批量评估，包含单个指标的评估请求信息
 *
 * @author qyl
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationRequest {
    
    /**
     * 指标编码
     */
    private String indicatorCode;
    
    /**
     * 指标版本
     */
    private String indicatorVersion;
    
    /**
     * 指标参数
     */
    private Map<String, Object> params;
}

