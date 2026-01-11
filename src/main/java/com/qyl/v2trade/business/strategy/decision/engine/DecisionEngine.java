package com.qyl.v2trade.business.strategy.decision.engine;

import com.qyl.v2trade.business.strategy.decision.event.*;
import com.qyl.v2trade.business.strategy.factory.model.StrategyInstance;
import com.qyl.v2trade.indicator.domain.event.BarClosedEvent;

/**
 * 决策引擎
 * 
 * <p>职责：
 * <ul>
 *   <li>协调整个决策流程</li>
 *   <li>事件驱动触发决策</li>
 *   <li>保证决策的原子性和幂等性</li>
 * </ul>
 * 
 * <p>阶段2约束：
 * <ul>
 *   <li>不修改 logic_state</li>
 *   <li>不发送交易指令</li>
 *   <li>只写 strategy_intent_record</li>
 * </ul>
 * 
 * <p>注意：这是占位接口，具体实现将在任务8.1完成
 */
public interface DecisionEngine {

    /**
     * 执行决策（信号触发）
     * 
     * @param instance 策略实例
     * @param event 信号意图激活事件
     */
    void executeDecision(StrategyInstance instance, SignalIntentActivatedEvent event);

    /**
     * 执行决策（指标触发）
     * 
     * @param instance 策略实例
     * @param event 指标计算完成事件
     */
    void executeDecision(StrategyInstance instance, IndicatorComputedEvent event);

    /**
     * 执行决策（K线触发）
     * 
     * @param instance 策略实例
     * @param event K线闭合事件
     */
    void executeDecision(StrategyInstance instance, BarClosedEvent event);

    /**
     * 执行决策（价格触发）
     * 
     * @param instance 策略实例
     * @param event 价格阈值穿越事件
     */
    void executeDecision(StrategyInstance instance, PriceTriggeredDecisionEvent event);
}

