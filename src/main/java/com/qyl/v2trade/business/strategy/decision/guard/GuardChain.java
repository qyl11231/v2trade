package com.qyl.v2trade.business.strategy.decision.guard;

import com.qyl.v2trade.business.strategy.decision.context.DecisionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 门禁链
 * 
 * <p>职责：
 * <ul>
 *   <li>按顺序执行所有门禁</li>
 *   <li>任一门禁拒绝则立即返回</li>
 *   <li>所有门禁通过才允许决策</li>
 * </ul>
 * 
 * <p>阶段2约束：
 * <ul>
 *   <li>门禁顺序：PhaseGate -> StalenessGate -> DedupGate -> SanityGate</li>
 *   <li>门禁失败不抛异常，只返回拒绝结果</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GuardChain {

    private final PhaseGate phaseGate;
    private final StalenessGate stalenessGate;
    private final DedupGate dedupGate;
    private final SanityGate sanityGate;

    /**
     * 执行门禁链校验
     * 
     * @param ctx 决策上下文
     * @return 校验结果
     */
    public GuardResult check(DecisionContext ctx) {
        if (ctx == null) {
            return GuardResult.rejected("GuardChain", "决策上下文为空");
        }

        // 按顺序执行门禁
        List<Gate> gates = List.of(phaseGate, stalenessGate, dedupGate, sanityGate);

        for (Gate gate : gates) {
            GuardResult result = gate.check(ctx);
            if (!result.isAllowed()) {
                log.debug("门禁拒绝: gate={}, strategyId={}, tradingPairId={}, reason={}",
                    gate.getName(), ctx.getStrategyId(), ctx.getTradingPairId(), result.getReason());
                return result;
            }
        }

        log.debug("所有门禁通过: strategyId={}, tradingPairId={}",
            ctx.getStrategyId(), ctx.getTradingPairId());

        return GuardResult.allowed();
    }
}

