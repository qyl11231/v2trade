package com.qyl.v2trade.business.strategy.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 策略实例实体
 */
@Data
@TableName(value = "strategy_instance", autoResultMap = true)
public class StrategyInstance implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 策略实例ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 策略ID（外键关联 strategy_definition.id）
     */
    private Long strategyId;

    /**
     * 绑定信号定义ID（0表示无信号绑定）
     */
    private Long signalConfigId;

    /**
     * 交易对ID（外键关联 trading_pair.id）
     */
    private Long tradingPairId;

    /**
     * 策略交易对（32字符，自动生成）
     */
    private String strategySymbol;

    /**
     * 策略初始资金（20位整数8位小数）
     */
    private BigDecimal initialCapital;

    /**
     * 策略大脑运行规则JSON（可为空，N4阶段使用）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String runtimeRules;

    /**
     * 策略止盈比例（10位整数4位小数，可为空）
     */
    private BigDecimal takeProfitRatio;

    /**
     * 策略止损比例（10位整数4位小数，可为空）
     */
    private BigDecimal stopLossRatio;

    /**
     * 版本号（默认1，每次更新自增）
     */
    private Integer version;

    /**
     * 是否启用：1-启用 0-禁用
     */
    private Integer enabled;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

