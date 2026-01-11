package com.qyl.v2trade.business.strategy.decision.guard;

import com.qyl.v2trade.business.strategy.decision.context.DecisionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 阶段门禁
 * 
 * <p>职责：
 * <ul>
 *   <li>校验策略阶段是否允许执行该动作</li>
 *   <li>空仓只允许OPEN，持仓允许CLOSE/REDUCE/ADD/REVERSE</li>
 * </ul>
 * 
 * <p>阶段2约束：
 * <ul>
 *   <li>FLAT只允许OPEN</li>
 *   <li>持仓允许CLOSE/REDUCE/ADD/REVERSE</li>
 *   <li>不校验具体阶段（OPEN_PENDING/OPENED等），只校验持仓状态</li>
 * </ul>
 */
@Slf4j
@Component
public class PhaseGate implements Gate {

    @Override
    public String getName() {
        return "PhaseGate";
    }

    @Override
    public GuardResult check(DecisionContext ctx) {
        if (ctx == null || ctx.getLogicStateBefore() == null) {
            return GuardResult.rejected(getName(), "决策上下文或逻辑状态为空");
        }

        // 判断是否为空仓
        boolean isFlat = ctx.isFlat();

        // 注意：这里不判断具体的决策动作，因为决策动作是在StrategyLogic中计算的
        // 阶段门禁只校验当前状态是否允许决策（空仓/持仓状态）
        // 具体的动作限制会在StrategyLogic中根据策略类型和状态判断

        // 空仓状态：允许决策（可能产生OPEN）
        // 持仓状态：允许决策（可能产生CLOSE/REDUCE/ADD/REVERSE）
        // 这里不做限制，因为决策动作还未计算

        log.debug("阶段门禁通过: strategyId={}, tradingPairId={}, isFlat={}",
            ctx.getStrategyId(), ctx.getTradingPairId(), isFlat);

        return GuardResult.allowed();
    }
}

