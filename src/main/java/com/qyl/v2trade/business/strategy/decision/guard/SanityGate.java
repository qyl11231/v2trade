package com.qyl.v2trade.business.strategy.decision.guard;

import com.qyl.v2trade.business.strategy.decision.context.DecisionContext;
import com.qyl.v2trade.business.strategy.decision.context.snapshot.ParamSnapshot;
import com.qyl.v2trade.business.strategy.decision.context.snapshot.PriceSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 合理性门禁
 * 
 * <p>职责：
 * <ul>
 *   <li>校验数据的合理性（价格、参数等）</li>
 *   <li>防止异常数据导致错误决策</li>
 * </ul>
 * 
 * <p>阶段2约束：
 * <ul>
 *   <li>价格必须大于0</li>
 *   <li>参数必须有效（initialCapital > 0, baseOrderRatio > 0）</li>
 *   <li>逻辑状态必须存在</li>
 * </ul>
 */
@Slf4j
@Component
public class SanityGate implements Gate {

    @Override
    public String getName() {
        return "SanityGate";
    }

    @Override
    public GuardResult check(DecisionContext ctx) {
        if (ctx == null) {
            return GuardResult.rejected(getName(), "决策上下文为空");
        }

        // 1. 校验逻辑状态
        if (ctx.getLogicStateBefore() == null) {
            return GuardResult.rejected(getName(), "逻辑状态为空");
        }

        // 2. 校验策略参数
        GuardResult paramCheck = checkParam(ctx);
        if (!paramCheck.isAllowed()) {
            return paramCheck;
        }

        // 3. 校验价格（如果价格快照存在）
        if (ctx.hasPrice()) {
            GuardResult priceCheck = checkPrice(ctx);
            if (!priceCheck.isAllowed()) {
                return priceCheck;
            }
        }

        log.debug("合理性门禁通过: strategyId={}, tradingPairId={}",
            ctx.getStrategyId(), ctx.getTradingPairId());

        return GuardResult.allowed();
    }

    /**
     * 校验策略参数
     */
    private GuardResult checkParam(DecisionContext ctx) {
        ParamSnapshot param = ctx.getParamSnapshot();
        if (param == null) {
            return GuardResult.rejected(getName(), "策略参数为空");
        }

        // 校验initialCapital
        if (param.getInitialCapital() == null || param.getInitialCapital().compareTo(BigDecimal.ZERO) <= 0) {
            return GuardResult.rejected(getName(), "初始资金无效: " + param.getInitialCapital());
        }

        // 校验baseOrderRatio
        if (param.getBaseOrderRatio() == null || param.getBaseOrderRatio().compareTo(BigDecimal.ZERO) <= 0) {
            return GuardResult.rejected(getName(), "下单比例无效: " + param.getBaseOrderRatio());
        }

        // 校验baseOrderRatio不超过1（100%）
        if (param.getBaseOrderRatio().compareTo(BigDecimal.ONE) > 0) {
            return GuardResult.rejected(getName(), "下单比例超过100%: " + param.getBaseOrderRatio());
        }

        return GuardResult.allowed();
    }

    /**
     * 校验价格
     */
    private GuardResult checkPrice(DecisionContext ctx) {
        PriceSnapshot price = ctx.getPriceSnapshot();
        if (price == null || !price.isAvailable()) {
            // 价格不可用不是错误，只是警告
            log.debug("价格快照不可用: strategyId={}, tradingPairId={}",
                ctx.getStrategyId(), ctx.getTradingPairId());
            return GuardResult.allowed();  // 允许通过，但会在reason中说明
        }

        // 校验价格必须大于0
        if (price.getCurrentPrice() == null || price.getCurrentPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return GuardResult.rejected(getName(), "价格无效: " + price.getCurrentPrice());
        }

        return GuardResult.allowed();
    }
}

