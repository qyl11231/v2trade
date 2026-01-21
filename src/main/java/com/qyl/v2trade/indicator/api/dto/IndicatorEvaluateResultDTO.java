package com.qyl.v2trade.indicator.api.dto;

import com.qyl.v2trade.indicator.runtime.IndicatorEvaluateResult;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 指标评估结果DTO（V2新增）
 * 
 * <p>用于API响应
 *
 * @author qyl
 */
@Data
public class IndicatorEvaluateResultDTO {
    
    /**
     * 是否有效
     */
    private boolean valid;
    
    /**
     * 数据来源（CACHE / COMPUTED）
     */
    private String source;
    
    /**
     * 指标计算值
     */
    private Map<String, BigDecimal> values;
    
    /**
     * 计算指纹
     */
    private String fingerprint;
    
    /**
     * 计算耗时（毫秒）
     */
    private Integer costMs;
    
    /**
     * 错误信息
     */
    private String errorMsg;
    
    /**
     * 从 IndicatorEvaluateResult 转换为 DTO
     */
    public static IndicatorEvaluateResultDTO fromResult(IndicatorEvaluateResult result) {
        if (result == null) {
            return null;
        }
        
        IndicatorEvaluateResultDTO dto = new IndicatorEvaluateResultDTO();
        dto.setValid(result.isValid());
        dto.setSource(result.getSource());
        dto.setValues(result.getValues());
        dto.setFingerprint(result.getFingerprint());
        dto.setCostMs(result.getCostMs());
        dto.setErrorMsg(result.getErrorMsg());
        return dto;
    }
}

