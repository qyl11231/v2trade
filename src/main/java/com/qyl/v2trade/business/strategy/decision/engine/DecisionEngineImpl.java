package com.qyl.v2trade.business.strategy.decision.engine;

import com.qyl.v2trade.business.strategy.decision.context.DecisionContext;
import com.qyl.v2trade.business.strategy.decision.event.*;
import com.qyl.v2trade.business.strategy.decision.guard.GuardChain;
import com.qyl.v2trade.business.strategy.decision.guard.GuardResult;
import com.qyl.v2trade.business.strategy.decision.logic.DecisionResult;
import com.qyl.v2trade.business.strategy.decision.logic.StrategyLogic;
import com.qyl.v2trade.business.strategy.decision.logic.StrategyLogicRegistry;
import com.qyl.v2trade.business.strategy.decision.recorder.IntentRecorder;
import com.qyl.v2trade.business.strategy.decision.sampler.AtomicSampler;
import com.qyl.v2trade.business.strategy.factory.model.StrategyInstance;
import com.qyl.v2trade.indicator.domain.event.BarClosedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 决策引擎实现
 * 
 * <p>职责：
 * <ul>
 *   <li>协调整个决策流程</li>
 *   <li>事件驱动触发决策</li>
 *   <li>保证决策的原子性和幂等性</li>
 * </ul>
 * 
 * <p>决策流程：
 * <ol>
 *   <li>AtomicSampler采集上下文</li>
 *   <li>GuardChain门禁校验</li>
 *   <li>StrategyLogic计算决策</li>
 *   <li>IntentRecorder落库（仅动作意图）</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DecisionEngineImpl implements DecisionEngine {

    private final AtomicSampler sampler;
    private final GuardChain guardChain;
    private final StrategyLogicRegistry logicRegistry;
    private final IntentRecorder recorder;

    @Override
    public void executeDecision(StrategyInstance instance, SignalIntentActivatedEvent event) {
        log.debug("执行决策（信号触发）: strategyId={}, tradingPairId={}, signalIntentId={}",
            instance.getStrategyId(), instance.getTradingPairId(), event.getSignalIntentId());

        try {
            // 1. 采集上下文
            DecisionContext ctx = sampler.sample(instance, event);

            // 2. 门禁校验
            GuardResult guardResult = guardChain.check(ctx);
            if (!guardResult.isAllowed()) {
                log.debug("决策被门禁拦截: strategyId={}, tradingPairId={}, gate={}, reason={}",
                    instance.getStrategyId(), instance.getTradingPairId(),
                    guardResult.getRejectedGate(), guardResult.getReason());
                return;
            }

            // 3. 策略逻辑计算
            StrategyLogic logic = logicRegistry.getLogic(ctx.getStrategyType());
            DecisionResult result = logic.decide(ctx);

            // 4. 落库（仅动作意图）
            if (result.isActionIntent()) {
                recorder.record(ctx, result);
            } else {
                log.debug("决策结果为HOLD，不落库: strategyId={}, tradingPairId={}",
                    instance.getStrategyId(), instance.getTradingPairId());
            }

        } catch (Exception e) {
            log.error("决策执行失败: strategyId={}, tradingPairId={}",
                instance.getStrategyId(), instance.getTradingPairId(), e);
            // 不抛异常，允许继续处理其他决策
        }
    }

    @Override
    public void executeDecision(StrategyInstance instance, IndicatorComputedEvent event) {
        log.debug("执行决策（指标触发）: strategyId={}, tradingPairId={}, indicatorCode={}",
            instance.getStrategyId(), instance.getTradingPairId(), event.getIndicatorCode());

        try {
            DecisionContext ctx = sampler.sample(instance, event);
            GuardResult guardResult = guardChain.check(ctx);
            if (!guardResult.isAllowed()) {
                log.debug("决策被门禁拦截: strategyId={}, tradingPairId={}, gate={}",
                    instance.getStrategyId(), instance.getTradingPairId(), guardResult.getRejectedGate());
                return;
            }

            StrategyLogic logic = logicRegistry.getLogic(ctx.getStrategyType());
            DecisionResult result = logic.decide(ctx);

            if (result.isActionIntent()) {
                recorder.record(ctx, result);
            }

        } catch (Exception e) {
            log.error("决策执行失败: strategyId={}, tradingPairId={}",
                instance.getStrategyId(), instance.getTradingPairId(), e);
        }
    }

    @Override
    public void executeDecision(StrategyInstance instance, BarClosedEvent event) {
        log.debug("执行决策（K线触发）: strategyId={}, tradingPairId={}, timeframe={}, barCloseTime={}",
            instance.getStrategyId(), instance.getTradingPairId(), event.timeframe(), event.barCloseTime());

        try {
            DecisionContext ctx = sampler.sample(instance, event);
            GuardResult guardResult = guardChain.check(ctx);
            if (!guardResult.isAllowed()) {
                log.debug("决策被门禁拦截: strategyId={}, tradingPairId={}, gate={}",
                    instance.getStrategyId(), instance.getTradingPairId(), guardResult.getRejectedGate());
                return;
            }

            StrategyLogic logic = logicRegistry.getLogic(ctx.getStrategyType());
            DecisionResult result = logic.decide(ctx);

            if (result.isActionIntent()) {
                recorder.record(ctx, result);
            }

        } catch (Exception e) {
            log.error("决策执行失败: strategyId={}, tradingPairId={}",
                instance.getStrategyId(), instance.getTradingPairId(), e);
        }
    }

    @Override
    public void executeDecision(StrategyInstance instance, PriceTriggeredDecisionEvent event) {
        log.debug("执行决策（价格触发）: strategyId={}, tradingPairId={}, triggerType={}, triggerPrice={}",
            instance.getStrategyId(), instance.getTradingPairId(), event.getTriggerType(), event.getTriggerPrice());

        try {
            DecisionContext ctx = sampler.sample(instance, event);
            GuardResult guardResult = guardChain.check(ctx);
            if (!guardResult.isAllowed()) {
                log.debug("决策被门禁拦截: strategyId={}, tradingPairId={}, gate={}",
                    instance.getStrategyId(), instance.getTradingPairId(), guardResult.getRejectedGate());
                return;
            }

            StrategyLogic logic = logicRegistry.getLogic(ctx.getStrategyType());
            DecisionResult result = logic.decide(ctx);

            if (result.isActionIntent()) {
                recorder.record(ctx, result);
            }

        } catch (Exception e) {
            log.error("决策执行失败: strategyId={}, tradingPairId={}",
                instance.getStrategyId(), instance.getTradingPairId(), e);
        }
    }
}

