package com.qyl.v2trade.business.strategy.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.qyl.v2trade.common.constants.LogicDirectionEnum;
import com.qyl.v2trade.common.constants.LogicPhaseEnum;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 策略逻辑状态实体
 * 
 * <p>映射 strategy_logic_state 表
 * 
 * <p>职责：
 * <ul>
 *   <li>记录策略对每个交易对的逻辑状态（唯一事实源）</li>
 *   <li>防止策略失忆，系统重启后可恢复状态</li>
 *   <li>每个策略+交易对组合有且仅有一条记录</li>
 * </ul>
 * 
 * <p>阶段1约束：
 * <ul>
 *   <li>状态变更必须立即持久化</li>
 *   <li>状态恢复必须幂等</li>
 * </ul>
 */
@Data
@TableName("strategy_logic_state")
public class StrategyLogicState implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 状态ID
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
     * 交易对ID
     */
    private Long tradingPairId;

    /**
     * 逻辑持仓方向：LONG / SHORT / FLAT
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
     * 策略阶段：
     * IDLE / OPEN_PENDING / OPENED / PARTIAL_EXIT / EXIT_PENDING / CLOSED
     */
    private String statePhase;

    /**
     * 最近一次关联的 signal_intent ID
     */
    private Long lastSignalIntentId;

    /**
     * 未实现盈亏（策略内部估算）
     */
    private BigDecimal unrealizedPnl;

    /**
     * 已实现盈亏
     */
    private BigDecimal realizedPnl;

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

    /**
     * 获取逻辑持仓方向枚举
     * 
     * @return 逻辑持仓方向枚举，如果为null返回null
     */
    public LogicDirectionEnum getLogicPositionSideEnum() {
        if (logicPositionSide == null) {
            return null;
        }
        return LogicDirectionEnum.fromCode(logicPositionSide);
    }

    /**
     * 设置逻辑持仓方向枚举
     * 
     * @param direction 逻辑持仓方向枚举
     */
    public void setLogicPositionSideEnum(LogicDirectionEnum direction) {
        if (direction == null) {
            this.logicPositionSide = null;
        } else {
            this.logicPositionSide = direction.getCode();
        }
    }

    /**
     * 获取策略阶段枚举
     * 
     * @return 策略阶段枚举，如果为null返回null
     */
    public LogicPhaseEnum getStatePhaseEnum() {
        if (statePhase == null) {
            return null;
        }
        return LogicPhaseEnum.fromCode(statePhase);
    }

    /**
     * 设置策略阶段枚举
     * 
     * @param phase 策略阶段枚举
     */
    public void setStatePhaseEnum(LogicPhaseEnum phase) {
        if (phase == null) {
            this.statePhase = null;
        } else {
            this.statePhase = phase.getCode();
        }
    }
}

