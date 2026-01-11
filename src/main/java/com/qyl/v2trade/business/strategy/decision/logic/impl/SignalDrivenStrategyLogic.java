package com.qyl.v2trade.business.strategy.decision.logic.impl;

import com.qyl.v2trade.business.strategy.decision.context.DecisionContext;
import com.qyl.v2trade.business.strategy.decision.context.snapshot.ParamSnapshot;
import com.qyl.v2trade.business.strategy.decision.context.snapshot.SignalSnapshot;
import com.qyl.v2trade.business.strategy.decision.logic.DecisionReason;
import com.qyl.v2trade.business.strategy.decision.logic.DecisionResult;
import com.qyl.v2trade.business.strategy.decision.logic.StrategyLogic;
import com.qyl.v2trade.common.constants.IntentActionEnum;
import com.qyl.v2trade.common.constants.LogicDirectionEnum;
import com.qyl.v2trade.common.constants.StrategyType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 信号驱动策略逻辑
 * 
 * <p>职责：
 * <ul>
 *   <li>实现信号驱动的策略逻辑</li>
 *   <li>根据信号方向决定动作</li>
 *   <li>空仓时：信号BUY → OPEN LONG，信号SELL → OPEN SHORT</li>
 *   <li>持仓时：反向信号 → CLOSE/REVERSE</li>
 * </ul>
 * 
 * <p>阶段2约束：
 * <ul>
 *   <li>不考虑指标条件（纯信号驱动）</li>
 *   <li>不计算复杂仓位（使用base_order_ratio）</li>
 *   <li>信号为空时返回HOLD（由GuardChain提前拦截）</li>
 * </ul>
 */
@Slf4j
@Component
public class SignalDrivenStrategyLogic implements StrategyLogic {

    @Override
    public String getSupportedStrategyType() {
        return StrategyType.SIGNAL_DRIVEN;
    }

    @Override
    public DecisionResult decide(DecisionContext ctx) {
        log.debug("执行信号驱动策略决策: strategyId={}, tradingPairId={}",
            ctx.getStrategyId(), ctx.getTradingPairId());

        // 1. 获取信号快照
        SignalSnapshot signal = ctx.getSignalSnapshot();
        if (signal == null || !signal.isValid()) {
            log.debug("信号无效或为空，返回HOLD: strategyId={}, tradingPairId={}",
                ctx.getStrategyId(), ctx.getTradingPairId());
            return DecisionResult.hold(buildHoldReason(ctx, "信号无效或为空"));
        }

        // 2. 获取参数快照
        ParamSnapshot param = ctx.getParamSnapshot();
        if (param == null) {
            log.warn("策略参数为空，返回HOLD: strategyId={}, tradingPairId={}",
                ctx.getStrategyId(), ctx.getTradingPairId());
            return DecisionResult.hold(buildHoldReason(ctx, "策略参数为空"));
        }

        // 3. 计算下单数量
        BigDecimal calculatedQty = param.calculateOrderQty();

        // 4. 判断当前持仓状态
        boolean isFlat = ctx.isFlat();
        LogicDirectionEnum currentPosition = ctx.getLogicStateBefore().getLogicPositionSideEnum();

        // 5. 根据信号方向和当前状态决定动作
        String signalDirection = signal.getIntentDirection();
        IntentActionEnum action = determineAction(signalDirection, isFlat, currentPosition);

        // 6. 构建决策原因
        DecisionReason reason = buildDecisionReason(ctx, signal, action, isFlat, currentPosition);

        log.info("信号驱动策略决策完成: strategyId={}, tradingPairId={}, action={}, qty={}, signalDirection={}",
            ctx.getStrategyId(), ctx.getTradingPairId(), action, calculatedQty, signalDirection);

        return DecisionResult.action(action, calculatedQty, reason);
    }

    /**
     * 根据信号方向和当前状态决定动作
     * 
     * <p>规则：
     * <ul>
     *   <li>空仓时：BUY → OPEN LONG，SELL → OPEN SHORT</li>
     *   <li>持仓LONG时：SELL → CLOSE，REVERSE → REVERSE</li>
     *   <li>持仓SHORT时：BUY → CLOSE，REVERSE → REVERSE</li>
     *   <li>其他情况 → HOLD</li>
     * </ul>
     */
    private IntentActionEnum determineAction(String signalDirection, boolean isFlat,
                                             LogicDirectionEnum currentPosition) {
        if (isFlat) {
            // 空仓状态
            if ("BUY".equals(signalDirection)) {
                return IntentActionEnum.OPEN;  // 开多
            } else if ("SELL".equals(signalDirection)) {
                return IntentActionEnum.OPEN;  // 开空
            } else if ("REVERSE".equals(signalDirection)) {
                // REVERSE信号在空仓时也开仓（根据信号方向决定多空）
                return IntentActionEnum.OPEN;
            } else {
                return IntentActionEnum.HOLD;
            }
        } else {
            // 持仓状态
            if (currentPosition == LogicDirectionEnum.LONG) {
                // 持多仓
                if ("SELL".equals(signalDirection) || "FLAT".equals(signalDirection)) {
                    return IntentActionEnum.CLOSE;  // 平多
                } else if ("REVERSE".equals(signalDirection)) {
                    return IntentActionEnum.REVERSE;  // 反手（平多开空）
                } else {
                    return IntentActionEnum.HOLD;
                }
            } else if (currentPosition == LogicDirectionEnum.SHORT) {
                // 持空仓
                if ("BUY".equals(signalDirection) || "FLAT".equals(signalDirection)) {
                    return IntentActionEnum.CLOSE;  // 平空
                } else if ("REVERSE".equals(signalDirection)) {
                    return IntentActionEnum.REVERSE;  // 反手（平空开多）
                } else {
                    return IntentActionEnum.HOLD;
                }
            } else {
                return IntentActionEnum.HOLD;
            }
        }
    }

    /**
     * 构建决策原因
     */
    private DecisionReason buildDecisionReason(DecisionContext ctx, SignalSnapshot signal,
                                              IntentActionEnum action, boolean isFlat,
                                              LogicDirectionEnum currentPosition) {
        Map<String, Object> decisionBasis = new HashMap<>();
        decisionBasis.put("signalDirection", signal.getIntentDirection());
        decisionBasis.put("signalIntentId", signal.getSignalIntentId());
        decisionBasis.put("currentPosition", currentPosition != null ? currentPosition.getCode() : "FLAT");
        decisionBasis.put("isFlat", isFlat);
        decisionBasis.put("action", action.getCode());

        String stateChange = String.format("信号方向=%s, 当前状态=%s, 决策动作=%s",
            signal.getIntentDirection(),
            currentPosition != null ? currentPosition.getCode() : "FLAT",
            action.getCode());

        return DecisionReason.builder()
            .triggerType(ctx.getTriggerType() != null ? ctx.getTriggerType().getCode() : "SIGNAL")
            .triggerSource(String.valueOf(signal.getSignalIntentId()))
            .triggerTimestamp(LocalDateTime.now().toString())
            .decisionBasis(decisionBasis)
            .stateChange(stateChange)
            .build();
    }

    /**
     * 构建HOLD原因
     */
    private DecisionReason buildHoldReason(DecisionContext ctx, String reason) {
        return DecisionReason.builder()
            .triggerType(ctx.getTriggerType() != null ? ctx.getTriggerType().getCode() : "SIGNAL")
            .triggerSource("HOLD")
            .triggerTimestamp(LocalDateTime.now().toString())
            .decisionBasis(Map.of("reason", reason))
            .stateChange("保持当前状态")
            .build();
    }
}

