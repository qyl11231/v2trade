package com.qyl.v2trade.business.strategy.decision.guard;

import com.qyl.v2trade.business.strategy.decision.context.DecisionContext;
import com.qyl.v2trade.business.strategy.decision.context.snapshot.IndicatorSnapshot;
import com.qyl.v2trade.business.strategy.decision.context.snapshot.SignalSnapshot;
import com.qyl.v2trade.common.constants.DecisionTriggerTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 时效门禁
 * 
 * <p>职责：
 * <ul>
 *   <li>校验数据时效性（信号、指标是否过期）</li>
 *   <li>校验数据是否就绪（指标值是否存在）</li>
 * </ul>
 * 
 * <p>阶段2约束：
 * <ul>
 *   <li>信号时效：30秒内有效（可配置）</li>
 *   <li>指标时效：不校验（指标值是事实表，持久化）</li>
 *   <li>指标就绪：如果是指标驱动策略，必须要有指标值</li>
 * </ul>
 */
@Slf4j
@Component
public class StalenessGate implements Gate {

    /**
     * 信号时效（秒），默认30秒
     */
    @Value("${strategy.decision.guard.signal.max-age-seconds:30}")
    private int signalMaxAgeSeconds;

    @Override
    public String getName() {
        return "StalenessGate";
    }

    @Override
    public GuardResult check(DecisionContext ctx) {
        if (ctx == null) {
            return GuardResult.rejected(getName(), "决策上下文为空");
        }

        // 1. 校验信号时效（如果是信号触发）
        if (ctx.getTriggerType() == DecisionTriggerTypeEnum.SIGNAL) {
            GuardResult signalCheck = checkSignalStaleness(ctx);
            if (!signalCheck.isAllowed()) {
                return signalCheck;
            }
        }

        // 2. 校验指标就绪（如果是指标驱动策略）
        if ("INDICATOR_DRIVEN".equals(ctx.getStrategyType()) || "HYBRID".equals(ctx.getStrategyType())) {
            GuardResult indicatorCheck = checkIndicatorReadiness(ctx);
            if (!indicatorCheck.isAllowed()) {
                return indicatorCheck;
            }
        }

        log.debug("时效门禁通过: strategyId={}, tradingPairId={}, triggerType={}",
            ctx.getStrategyId(), ctx.getTradingPairId(), ctx.getTriggerType());

        return GuardResult.allowed();
    }

    /**
     * 校验信号时效
     */
    private GuardResult checkSignalStaleness(DecisionContext ctx) {
        SignalSnapshot signal = ctx.getSignalSnapshot();
        if (signal == null) {
            return GuardResult.rejected(getName(), "信号触发但信号快照为空");
        }

        // 检查信号是否有效
        if (!signal.isValid()) {
            return GuardResult.rejected(getName(), "信号无效: status=" + signal.getIntentStatus());
        }

        // 检查信号是否过期（基于receivedAt）
        if (signal.getReceivedAt() != null) {
            LocalDateTime now = LocalDateTime.now();
            Duration age = Duration.between(signal.getReceivedAt(), now);
            if (age.getSeconds() > signalMaxAgeSeconds) {
                return GuardResult.rejected(getName(),
                    String.format("信号已过期: age=%d秒, maxAge=%d秒", age.getSeconds(), signalMaxAgeSeconds));
            }
        }

        return GuardResult.allowed();
    }

    /**
     * 校验指标就绪
     */
    private GuardResult checkIndicatorReadiness(DecisionContext ctx) {
        IndicatorSnapshot indicator = ctx.getIndicatorSnapshot();
        if (indicator == null || !indicator.hasValue()) {
            return GuardResult.rejected(getName(), "指标驱动策略但指标值未就绪");
        }

        // 指标值存在，允许通过
        return GuardResult.allowed();
    }
}

