package com.qyl.v2trade.indicator.api.dto;

import lombok.Data;

import java.util.Map;

/**
 * 创建订阅请求DTO
 *
 * @author qyl
 */
@Data
public class SubscriptionCreateRequest {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 交易对ID
     */
    private Long tradingPairId;
    
    /**
     * 周期（5m、15m、30m、1h、4h）
     */
    private String timeframe;
    
    /**
     * 指标编码
     */
    private String indicatorCode;
    
    /**
     * 指标版本
     */
    private String indicatorVersion;
    
    /**
     * 参数（JSON对象）
     */
    private Map<String, Object> params;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
}

