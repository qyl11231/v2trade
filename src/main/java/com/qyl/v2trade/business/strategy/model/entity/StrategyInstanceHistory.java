package com.qyl.v2trade.business.strategy.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 策略实例历史记录实体
 */
@Data
@TableName(value = "strategy_instance_history", autoResultMap = true)
public class StrategyInstanceHistory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 历史记录ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
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
     * 策略实例ID（外键关联 strategy_instance.id）
     */
    private Long strategyInstanceId;

    /**
     * 绑定信号定义ID（0表示无信号绑定）
     */
    private Long signalConfigId;

    /**
     * 交易对ID
     */
    private Long tradingPairId;

    /**
     * 策略交易对（32字符）
     */
    private String strategySymbol;

    /**
     * 策略初始资金
     */
    private BigDecimal initialCapital;

    /**
     * 策略运行规则JSON
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

