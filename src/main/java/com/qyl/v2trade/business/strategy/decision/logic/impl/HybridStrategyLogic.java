package com.qyl.v2trade.business.strategy.decision.logic.impl;

import com.qyl.v2trade.business.strategy.decision.context.DecisionContext;
import com.qyl.v2trade.business.strategy.decision.logic.DecisionResult;
import com.qyl.v2trade.business.strategy.decision.logic.StrategyLogic;
import com.qyl.v2trade.common.constants.StrategyType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 混合策略逻辑
 * 
 * <p>职责：
 * <ul>
 *   <li>实现信号+指标的混合策略逻辑</li>
 *   <li>同时考虑信号和指标</li>
 *   <li>支持ALL/ANY模式</li>
 * </ul>
 * 
 * <p>阶段2约束：
 * <ul>
 *   <li>不实现复杂的权重计算（MVP只支持ALL/ANY）</li>
 *   <li>ALL模式：信号和指标都满足才触发</li>
 *   <li>ANY模式：信号或指标任一满足就触发</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HybridStrategyLogic implements StrategyLogic {

    private final SignalDrivenStrategyLogic signalLogic;
    private final IndicatorDrivenStrategyLogic indicatorLogic;

    @Override
    public String getSupportedStrategyType() {
        return StrategyType.HYBRID;
    }

    @Override
    public DecisionResult decide(DecisionContext ctx) {
        log.debug("执行混合策略决策: strategyId={}, tradingPairId={}",
            ctx.getStrategyId(), ctx.getTradingPairId());

        // 1. 分别执行信号驱动和指标驱动的决策
        DecisionResult signalResult = signalLogic.decide(ctx);
        DecisionResult indicatorResult = indicatorLogic.decide(ctx);

        // 2. 判断模式（ALL/ANY）
        // TODO: 从策略参数中读取模式，当前默认使用ANY模式
        boolean useAllMode = false;  // 默认ANY模式

        // 3. 根据模式合并结果
        DecisionResult finalResult;
        if (useAllMode) {
            // ALL模式：两个结果都必须是动作意图才触发
            if (signalResult.isActionIntent() && indicatorResult.isActionIntent()) {
                // 两个都满足，使用信号驱动的结果（优先级更高）
                finalResult = signalResult;
            } else {
                // 任一不满足，返回HOLD
                finalResult = DecisionResult.hold(signalResult.getReason());
            }
        } else {
            // ANY模式：任一满足就触发
            if (signalResult.isActionIntent()) {
                finalResult = signalResult;  // 信号满足，使用信号结果
            } else if (indicatorResult.isActionIntent()) {
                finalResult = indicatorResult;  // 指标满足，使用指标结果
            } else {
                // 都不满足，返回HOLD
                finalResult = DecisionResult.hold(signalResult.getReason());
            }
        }

        log.info("混合策略决策完成: strategyId={}, tradingPairId={}, action={}, mode={}, signalAction={}, indicatorAction={}",
            ctx.getStrategyId(), ctx.getTradingPairId(), finalResult.getAction(),
            useAllMode ? "ALL" : "ANY", signalResult.getAction(), indicatorResult.getAction());

        return finalResult;
    }
}

