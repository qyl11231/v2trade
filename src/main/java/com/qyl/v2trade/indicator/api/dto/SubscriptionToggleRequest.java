package com.qyl.v2trade.indicator.api.dto;

import lombok.Data;

/**
 * 启停订阅请求DTO
 *
 * @author qyl
 */
@Data
public class SubscriptionToggleRequest {
    
    /**
     * 是否启用
     */
    private Boolean enabled;
}

