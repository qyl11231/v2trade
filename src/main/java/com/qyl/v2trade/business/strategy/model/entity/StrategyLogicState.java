package com.qyl.v2trade.business.strategy.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 策略逻辑状态实体
 * 
 * <p>映射 strategy_logic_state 表，用于持久化策略运行状态快照
 *
 * @author qyl
 */
@Data
@TableName("strategy_logic_state")
public class StrategyLogicState implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
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
     * 策略实例ID（外键关联 strategy_instance.id，唯一键）
     */
    private Long strategyInstanceId;

    /**
     * 交易对ID（外键关联 trading_pair.id）
     */
    private Long tradingPairId;

    /**
     * 策略交易对（32字符）
     */
    private String strategySymbol;

    /**
     * 逻辑持仓方向：LONG做多 SHORT做空 FLAT空仓
     */
    private String logicPositionSide;

    /**
     * 逻辑持仓数量（策略计算结果）
     */
    private BigDecimal logicPositionQty;

    /**
     * 逻辑平均开仓价
     */
    private BigDecimal avgEntryPrice;

    /**
     * 策略阶段：IDLE/OPEN_PENDING/OPENED/PARTIAL_EXIT/EXIT_PENDING/ADD_PENDING/CLOSED
     */
    private String statePhase;

    /**
     * 未实现盈亏（N3 阶段不写，等 N7 执行回执后写）
     */
    private BigDecimal unrealizedPnl;

    /**
     * 已实现盈亏（N3 阶段不写，等 N7 执行回执后写）
     */
    private BigDecimal realizedPnl;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

