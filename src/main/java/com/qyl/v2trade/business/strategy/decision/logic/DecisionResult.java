package com.qyl.v2trade.business.strategy.decision.logic;

import com.qyl.v2trade.common.constants.IntentActionEnum;
import lombok.Builder;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 决策结果
 * 
 * <p>职责：
 * <ul>
 *   <li>表示策略逻辑计算的结果</li>
 *   <li>包含决策动作和计算出的数量</li>
 *   <li>包含决策原因（用于构建decision_reason JSON）</li>
 * </ul>
 * 
 * <p>阶段2约束：
 * <ul>
 *   <li>只有动作意图才落库（OPEN/CLOSE/ADD/REDUCE/REVERSE）</li>
 *   <li>HOLD不落库（只记录metrics/log）</li>
 * </ul>
 */
@Getter
@Builder
public class DecisionResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 决策意图：OPEN / CLOSE / ADD / REDUCE / REVERSE / HOLD
     */
    private final IntentActionEnum action;

    /**
     * 计算出的下单数量
     * 
     * <p>基于策略参数（initial_capital * base_order_ratio）计算
     */
    private final BigDecimal calculatedQty;

    /**
     * 决策原因（结构化信息，用于构建decision_reason JSON）
     * 
     * <p>包含：
     * <ul>
     *   <li>触发源信息</li>
     *   <li>决策依据（信号方向、指标值、条件等）</li>
     *   <li>状态变化说明</li>
     * </ul>
     */
    private final DecisionReason reason;

    /**
     * 创建HOLD结果
     * 
     * @param reason 原因
     * @return HOLD结果
     */
    public static DecisionResult hold(DecisionReason reason) {
        return DecisionResult.builder()
            .action(IntentActionEnum.HOLD)
            .calculatedQty(BigDecimal.ZERO)
            .reason(reason)
            .build();
    }

    /**
     * 创建动作结果
     * 
     * @param action 动作
     * @param calculatedQty 计算出的数量
     * @param reason 原因
     * @return 动作结果
     */
    public static DecisionResult action(IntentActionEnum action, BigDecimal calculatedQty, DecisionReason reason) {
        return DecisionResult.builder()
            .action(action)
            .calculatedQty(calculatedQty)
            .reason(reason)
            .build();
    }

    /**
     * 判断是否为动作意图（需要落库）
     */
    public boolean isActionIntent() {
        return action != null && action.isActionIntent();
    }
}

