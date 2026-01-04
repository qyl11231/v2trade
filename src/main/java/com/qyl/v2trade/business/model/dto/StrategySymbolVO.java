package com.qyl.v2trade.business.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 策略交易对视图对象
 */
@Data
public class StrategySymbolVO {

    /**
     * 主键
     */
    private Long id;

    /**
     * 策略ID
     */
    private Long strategyId;

    /**
     * 交易对ID
     */
    private Long tradingPairId;

    /**
     * 交易对名称（来自关联表）
     */
    private String tradingPairName;

    /**
     * 市场类型（来自关联表）
     */
    private String marketType;

    /**
     * 是否启用
     */
    private Integer enabled;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

