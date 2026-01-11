package com.qyl.v2trade.business.strategy.decision.logic.impl;

import com.qyl.v2trade.business.strategy.decision.context.DecisionContext;
import com.qyl.v2trade.business.strategy.decision.context.snapshot.IndicatorSnapshot;
import com.qyl.v2trade.business.strategy.decision.context.snapshot.ParamSnapshot;
import com.qyl.v2trade.business.strategy.decision.logic.DecisionReason;
import com.qyl.v2trade.business.strategy.decision.logic.DecisionResult;
import com.qyl.v2trade.business.strategy.decision.logic.StrategyLogic;
import com.qyl.v2trade.business.strategy.decision.logic.condition.EntryConditionEvaluator;
import com.qyl.v2trade.business.strategy.decision.logic.condition.EvaluationResult;
import com.qyl.v2trade.business.strategy.decision.logic.condition.ExitConditionEvaluator;
import com.qyl.v2trade.common.constants.IntentActionEnum;
import com.qyl.v2trade.common.constants.LogicDirectionEnum;
import com.qyl.v2trade.common.constants.StrategyType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 指标驱动策略逻辑
 * 
 * <p>职责：
 * <ul>
 *   <li>实现指标驱动的策略逻辑</li>
 *   <li>读取指标值（从IndicatorSnapshot）</li>
 *   <li>解析entry_condition和exit_condition（JSON）</li>
 *   <li>根据条件判断决定动作</li>
 * </ul>
 * 
 * <p>阶段2约束：
 * <ul>
 *   <li>不实现复杂的指标组合（MVP只支持简单条件）</li>
 *   <li>不实现自定义指标（使用现有指标）</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IndicatorDrivenStrategyLogic implements StrategyLogic {

    private final EntryConditionEvaluator entryConditionEvaluator;
    private final ExitConditionEvaluator exitConditionEvaluator;

    @Override
    public String getSupportedStrategyType() {
        return StrategyType.INDICATOR_DRIVEN;
    }

    @Override
    public DecisionResult decide(DecisionContext ctx) {
        log.debug("执行指标驱动策略决策: strategyId={}, tradingPairId={}",
            ctx.getStrategyId(), ctx.getTradingPairId());

        // 1. 获取指标快照
        IndicatorSnapshot indicator = ctx.getIndicatorSnapshot();
        if (indicator == null || !indicator.hasValue()) {
            log.debug("指标快照为空或无效，返回HOLD: strategyId={}, tradingPairId={}",
                ctx.getStrategyId(), ctx.getTradingPairId());
            return DecisionResult.hold(buildHoldReason(ctx, "指标快照为空或无效"));
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

        // 5. 评估条件并决定动作
        IntentActionEnum action;
        EvaluationResult conditionResult;
        if (isFlat) {
            // 空仓状态：评估入场条件
            conditionResult = entryConditionEvaluator.evaluate(ctx, param.getEntryCondition());
            if (conditionResult.isBlocked()) {
                // 条件被block（缺值且nullable=false），返回HOLD
                log.warn("入场条件被block: strategyId={}, tradingPairId={}, reason={}",
                    ctx.getStrategyId(), ctx.getTradingPairId(), conditionResult.getBlockReason());
                return DecisionResult.hold(buildHoldReason(ctx, "入场条件被block: " + conditionResult.getBlockReason()));
            }
            if (conditionResult.isPassed()) {
                action = IntentActionEnum.OPEN;  // 满足入场条件，开仓
            } else {
                action = IntentActionEnum.HOLD;  // 不满足入场条件，保持空仓
            }
        } else {
            // 持仓状态：评估出场条件
            conditionResult = exitConditionEvaluator.evaluate(ctx, param.getExitCondition());
            if (conditionResult.isBlocked()) {
                // 条件被block，返回HOLD
                log.warn("出场条件被block: strategyId={}, tradingPairId={}, reason={}",
                    ctx.getStrategyId(), ctx.getTradingPairId(), conditionResult.getBlockReason());
                return DecisionResult.hold(buildHoldReason(ctx, "出场条件被block: " + conditionResult.getBlockReason()));
            }
            if (conditionResult.isPassed()) {
                action = IntentActionEnum.CLOSE;  // 满足出场条件，平仓
            } else {
                action = IntentActionEnum.HOLD;  // 不满足出场条件，保持持仓
            }
        }

        // 6. 构建决策原因
        DecisionReason reason = buildDecisionReason(ctx, indicator, param, action, isFlat, currentPosition, conditionResult);

        log.info("指标驱动策略决策完成: strategyId={}, tradingPairId={}, action={}, qty={}, indicatorCode={}",
            ctx.getStrategyId(), ctx.getTradingPairId(), action, calculatedQty, indicator.getIndicatorCode());

        return DecisionResult.action(action, calculatedQty, reason);
    }

    /**
     * 构建决策原因
     */
    private DecisionReason buildDecisionReason(DecisionContext ctx, IndicatorSnapshot indicator,
                                              ParamSnapshot param, IntentActionEnum action,
                                              boolean isFlat, LogicDirectionEnum currentPosition,
                                              EvaluationResult conditionResult) {
        Map<String, Object> decisionBasis = new HashMap<>();
        decisionBasis.put("indicatorCode", indicator.getIndicatorCode());
        decisionBasis.put("indicatorValue", indicator.getValue());
        decisionBasis.put("indicatorBarTime", indicator.getBarTime());
        decisionBasis.put("currentPosition", currentPosition != null ? currentPosition.getCode() : "FLAT");
        decisionBasis.put("isFlat", isFlat);
        decisionBasis.put("action", action.getCode());

        if (isFlat) {
            decisionBasis.put("entryCondition", param.getEntryCondition());
            decisionBasis.put("entryConditionMet", action == IntentActionEnum.OPEN);
        } else {
            decisionBasis.put("exitCondition", param.getExitCondition());
            decisionBasis.put("exitConditionMet", action == IntentActionEnum.CLOSE);
        }
        
        // 添加hitReason（用于审计）
        if (conditionResult != null && conditionResult.getHitReason() != null) {
            decisionBasis.put("hitReason", conditionResult.getHitReason());
        }

        String stateChange = String.format("指标=%s, 值=%.2f, 当前状态=%s, 决策动作=%s",
            indicator.getIndicatorCode(),
            indicator.getValue(),
            currentPosition != null ? currentPosition.getCode() : "FLAT",
            action.getCode());

        return DecisionReason.builder()
            .triggerType(ctx.getTriggerType() != null ? ctx.getTriggerType().getCode() : "INDICATOR")
            .triggerSource(indicator.getIndicatorCode())
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
            .triggerType(ctx.getTriggerType() != null ? ctx.getTriggerType().getCode() : "INDICATOR")
            .triggerSource("HOLD")
            .triggerTimestamp(LocalDateTime.now().toString())
            .decisionBasis(Map.of("reason", reason))
            .stateChange("保持当前状态")
            .build();
    }
}

