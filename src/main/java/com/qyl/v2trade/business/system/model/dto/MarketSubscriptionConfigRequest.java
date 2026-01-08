package com.qyl.v2trade.business.system.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 行情订阅配置请求DTO
 */
@Data
public class MarketSubscriptionConfigRequest {

    /**
     * 交易对ID
     */
    @NotNull(message = "交易对ID不能为空")
    private Long tradingPairId;

    /**
     * 是否启用：1-启用 0-禁用
     */
    private Integer enabled;

    /**
     * Redis缓存时长（分钟）
     */
    @Min(value = 1, message = "缓存时长必须大于0")
    private Integer cacheDurationMinutes;

    /**
     * 备注说明
     */
    private String remark;
}

