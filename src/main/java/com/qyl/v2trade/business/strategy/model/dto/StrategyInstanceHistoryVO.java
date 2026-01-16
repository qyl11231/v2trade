package com.qyl.v2trade.business.strategy.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 策略实例历史记录视图对象
 */
@Data
public class StrategyInstanceHistoryVO {

    /**
     * 历史记录ID
     */
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 策略ID
     */
    private Long strategyId;

    /**
     * 策略实例ID
     */
    private Long strategyInstanceId;

    /**
     * 绑定信号定义ID（0表示无信号绑定）
     */
    private Long signalConfigId;

    /**
     * 信号配置名称（关联查询，可选）
     */
    private String signalConfigName;

    /**
     * 交易对ID
     */
    private Long tradingPairId;

    /**
     * 交易对符号（关联查询）
     */
    private String tradingPairSymbol;

    /**
     * 策略交易对
     */
    private String strategySymbol;

    /**
     * 策略初始资金
     */
    private BigDecimal initialCapital;

    /**
     * 策略运行规则JSON（字符串）
     */
    private String runtimeRules;

    /**
     * 策略止盈比例
     */
    private BigDecimal takeProfitRatio;

    /**
     * 策略止损比例
     */
    private BigDecimal stopLossRatio;

    /**
     * 版本号（保存历史时的版本）
     */
    private Integer version;

    /**
     * 历史记录创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

