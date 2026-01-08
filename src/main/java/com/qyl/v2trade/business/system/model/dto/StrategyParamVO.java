package com.qyl.v2trade.business.system.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 策略参数视图对象
 */
@Data
public class StrategyParamVO {

    /**
     * 参数ID
     */
    private Long id;

    /**
     * 策略ID
     */
    private Long strategyId;

    /**
     * 策略初始虚拟资金
     */
    private BigDecimal initialCapital;

    /**
     * 单次下单资金占比
     */
    private BigDecimal baseOrderRatio;

    /**
     * 策略止盈比例
     */
    private BigDecimal takeProfitRatio;

    /**
     * 策略止损比例
     */
    private BigDecimal stopLossRatio;

    /**
     * 策略入场条件（JSON字符串）
     */
    private String entryCondition;

    /**
     * 策略退出条件（JSON字符串）
     */
    private String exitCondition;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

