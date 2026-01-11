package com.qyl.v2trade.business.strategy.decision.context.snapshot;

import com.qyl.v2trade.common.constants.LogicDirectionEnum;
import com.qyl.v2trade.common.constants.LogicPhaseEnum;
import lombok.Builder;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 逻辑状态快照（不可变）
 * 
 * <p>从 strategy_logic_state 表读取的状态快照
 * 
 * <p>用于决策时的状态判断
 */
@Getter
@Builder
public class LogicStateSnapshot implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 逻辑持仓方向：LONG / SHORT / FLAT
     */
    private final String logicPositionSide;

    /**
     * 逻辑持仓数量
     */
    private final BigDecimal logicPositionQty;

    /**
     * 逻辑平均开仓价
     */
    private final BigDecimal avgEntryPrice;

    /**
     * 策略阶段：IDLE / OPEN_PENDING / OPENED / PARTIAL_EXIT / EXIT_PENDING / CLOSED
     */
    private final String statePhase;

    /**
     * 最近一次关联的 signal_intent ID
     */
    private final Long lastSignalIntentId;

    /**
     * 未实现盈亏
     */
    private final BigDecimal unrealizedPnl;

    /**
     * 已实现盈亏
     */
    private final BigDecimal realizedPnl;

    /**
     * 获取逻辑持仓方向枚举
     */
    public LogicDirectionEnum getLogicPositionSideEnum() {
        if (logicPositionSide == null) {
            return null;
        }
        return LogicDirectionEnum.fromCode(logicPositionSide);
    }

    /**
     * 获取策略阶段枚举
     */
    public LogicPhaseEnum getStatePhaseEnum() {
        if (statePhase == null) {
            return null;
        }
        return LogicPhaseEnum.fromCode(statePhase);
    }

    /**
     * 判断是否为空仓
     */
    public boolean isFlat() {
        return LogicDirectionEnum.FLAT.getCode().equals(logicPositionSide) ||
               (logicPositionQty != null && logicPositionQty.compareTo(BigDecimal.ZERO) == 0);
    }

    /**
     * 判断是否持仓
     */
    public boolean hasPosition() {
        return !isFlat();
    }
}

