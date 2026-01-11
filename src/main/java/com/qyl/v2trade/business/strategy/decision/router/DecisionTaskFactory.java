package com.qyl.v2trade.business.strategy.decision.router;

import com.qyl.v2trade.business.strategy.decision.event.*;
import com.qyl.v2trade.business.strategy.decision.engine.DecisionEngine;
import com.qyl.v2trade.business.strategy.factory.model.StrategyInstance;
import com.qyl.v2trade.indicator.domain.event.BarClosedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 决策任务工厂
 * 
 * <p>职责：
 * <ul>
 *   <li>根据事件类型创建决策任务</li>
 *   <li>封装决策任务的创建逻辑</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DecisionTaskFactory {

    private final DecisionEngine decisionEngine;

    /**
     * 创建决策任务
     * 
     * @param instance 策略实例
     * @param event 触发事件（四类事件之一）
     * @return 决策任务（Runnable）
     */
    public Runnable createDecisionTask(StrategyInstance instance, Object event) {
        if (instance == null) {
            throw new IllegalArgumentException("策略实例不能为null");
        }
        if (event == null) {
            throw new IllegalArgumentException("事件不能为null");
        }

        // 根据事件类型创建对应的决策任务
        if (event instanceof SignalIntentActivatedEvent) {
            return () -> decisionEngine.executeDecision(instance, (SignalIntentActivatedEvent) event);
        } else if (event instanceof IndicatorComputedEvent) {
            return () -> decisionEngine.executeDecision(instance, (IndicatorComputedEvent) event);
        } else if (event instanceof BarClosedEvent) {
            return () -> decisionEngine.executeDecision(instance, (BarClosedEvent) event);
        } else if (event instanceof PriceTriggeredDecisionEvent) {
            return () -> decisionEngine.executeDecision(instance, (PriceTriggeredDecisionEvent) event);
        } else {
            throw new IllegalArgumentException("不支持的事件类型: " + event.getClass().getName());
        }
    }
}

