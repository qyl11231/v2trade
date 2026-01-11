package com.qyl.v2trade.business.strategy.decision.logic.condition;

import com.qyl.v2trade.business.strategy.decision.context.DecisionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 出场条件评估器
 * 
 * <p>职责：
 * <ul>
 *   <li>评估策略的出场条件</li>
 *   <li>用于判断是否应该平仓</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExitConditionEvaluator {

    private final ConditionEvaluator conditionEvaluator;

    /**
     * 评估出场条件
     * 
     * @param ctx 决策上下文
     * @param exitConditionJson 出场条件JSON
     * @return 评估结果（包含passed、blocked、hitReason）
     */
    public EvaluationResult evaluate(DecisionContext ctx, String exitConditionJson) {
        if (exitConditionJson == null || exitConditionJson.isEmpty()) {
            log.debug("出场条件为空，返回blocked");
            return EvaluationResult.blocked("出场条件为空");
        }

        return conditionEvaluator.evaluate(ctx, exitConditionJson);
    }
}

