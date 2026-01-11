package com.qyl.v2trade.business.strategy.decision.router;

import com.qyl.v2trade.business.strategy.decision.event.*;
import com.qyl.v2trade.business.strategy.decision.executor.InstanceKey;
import com.qyl.v2trade.business.strategy.decision.executor.StripedExecutor;
import com.qyl.v2trade.business.strategy.factory.model.StrategyInstance;
import com.qyl.v2trade.indicator.domain.event.BarClosedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 决策事件路由器
 * 
 * <p>职责：
 * <ul>
 *   <li>订阅四类决策事件</li>
 *   <li>找到受影响的策略实例</li>
 *   <li>投递到StripedExecutor执行</li>
 * </ul>
 * 
 * <p>阶段2约束：
 * <ul>
 *   <li>事件驱动，禁止定时扫描</li>
 *   <li>找不到策略实例时记录日志，不抛异常</li>
 *   <li>所有事件处理都是异步的</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DecisionEventRouter {

    private final StrategyInstanceLocator instanceLocator;
    private final StripedExecutor stripedExecutor;
    private final DecisionTaskFactory taskFactory;

    /**
     * 处理信号意图激活事件
     * 
     * <p>路由规则：根据strategyId + tradingPairId查找StrategyInstance
     * 
     * @param event 信号意图激活事件
     */
    @Async("decisionEventExecutor")
    @EventListener
    public void handleSignalIntentActivated(SignalIntentActivatedEvent event) {
        log.debug("收到信号意图激活事件: strategyId={}, tradingPairId={}, signalIntentId={}",
            event.getStrategyId(), event.getTradingPairId(), event.getSignalIntentId());

        try {
            StrategyInstance instance = instanceLocator.locateByStrategyAndPair(
                event.getStrategyId(), event.getTradingPairId()
            );

            if (instance == null) {
                log.warn("找不到策略实例，事件将被忽略: strategyId={}, tradingPairId={}",
                    event.getStrategyId(), event.getTradingPairId());
                return;
            }

            // 投递到StripedExecutor
            InstanceKey instanceKey = InstanceKey.of(instance.getStrategyId(), instance.getTradingPairId());
            Runnable task = taskFactory.createDecisionTask(instance, event);
            stripedExecutor.submit(instanceKey, task);

            log.debug("信号意图激活事件已路由: strategyId={}, tradingPairId={}",
                event.getStrategyId(), event.getTradingPairId());

        } catch (Exception e) {
            log.error("处理信号意图激活事件失败: strategyId={}, tradingPairId={}",
                event.getStrategyId(), event.getTradingPairId(), e);
            // 不抛异常，允许继续处理其他事件
        }
    }

    /**
     * 处理指标计算完成事件
     * 
     * <p>路由规则：根据tradingPairId查找所有订阅该指标的策略实例
     * 
     * @param event 指标计算完成事件
     */
    @Async("decisionEventExecutor")
    @EventListener
    public void handleIndicatorComputed(IndicatorComputedEvent event) {
        log.debug("收到指标计算完成事件: tradingPairId={}, indicatorCode={}, barTime={}",
            event.getTradingPairId(), event.getIndicatorCode(), event.getBarTime());

        try {
            List<StrategyInstance> instances = instanceLocator.locateByTradingPairAndIndicator(
                event.getTradingPairId(), event.getIndicatorCode()
            );

            if (instances.isEmpty()) {
                log.debug("没有策略实例订阅该指标: tradingPairId={}, indicatorCode={}",
                    event.getTradingPairId(), event.getIndicatorCode());
                return;
            }

            // 为每个实例投递任务
            for (StrategyInstance instance : instances) {
                InstanceKey instanceKey = InstanceKey.of(instance.getStrategyId(), instance.getTradingPairId());
                Runnable task = taskFactory.createDecisionTask(instance, event);
                stripedExecutor.submit(instanceKey, task);
            }

            log.debug("指标计算完成事件已路由: tradingPairId={}, indicatorCode={}, instanceCount={}",
                event.getTradingPairId(), event.getIndicatorCode(), instances.size());

        } catch (Exception e) {
            log.error("处理指标计算完成事件失败: tradingPairId={}, indicatorCode={}",
                event.getTradingPairId(), event.getIndicatorCode(), e);
            // 不抛异常，允许继续处理其他事件
        }
    }

    /**
     * 处理K线闭合事件
     * 
     * <p>路由规则：根据tradingPairId查找所有在该交易对上运行的策略实例
     * 
     * @param event K线闭合事件
     */
    @Async("decisionEventExecutor")
    @EventListener
    public void handleBarClosed(BarClosedEvent event) {
        log.debug("收到K线闭合事件: tradingPairId={}, timeframe={}, barCloseTime={}",
            event.tradingPairId(), event.timeframe(), event.barCloseTime());

        try {
            List<StrategyInstance> instances = instanceLocator.locateByTradingPair(event.tradingPairId());

            if (instances.isEmpty()) {
                log.debug("没有策略实例在该交易对上运行: tradingPairId={}", event.tradingPairId());
                return;
            }

            // 为每个实例投递任务
            for (StrategyInstance instance : instances) {
                InstanceKey instanceKey = InstanceKey.of(instance.getStrategyId(), instance.getTradingPairId());
                Runnable task = taskFactory.createDecisionTask(instance, event);
                stripedExecutor.submit(instanceKey, task);
            }

            log.debug("K线闭合事件已路由: tradingPairId={}, timeframe={}, instanceCount={}",
                event.tradingPairId(), event.timeframe(), instances.size());

        } catch (Exception e) {
            log.error("处理K线闭合事件失败: tradingPairId={}, timeframe={}",
                event.tradingPairId(), event.timeframe(), e);
            // 不抛异常，允许继续处理其他事件
        }
    }

    /**
     * 处理价格阈值穿越事件
     * 
     * <p>路由规则：根据strategyId + tradingPairId直接定位StrategyInstance
     * 
     * @param event 价格阈值穿越事件
     */
    @Async("decisionEventExecutor")
    @EventListener
    public void handlePriceTriggered(PriceTriggeredDecisionEvent event) {
        log.debug("收到价格阈值穿越事件: strategyId={}, tradingPairId={}, triggerType={}, triggerPrice={}",
            event.getStrategyId(), event.getTradingPairId(), event.getTriggerType(), event.getTriggerPrice());

        try {
            StrategyInstance instance = instanceLocator.locateByStrategyAndPair(
                event.getStrategyId(), event.getTradingPairId()
            );

            if (instance == null) {
                log.warn("找不到策略实例，事件将被忽略: strategyId={}, tradingPairId={}",
                    event.getStrategyId(), event.getTradingPairId());
                return;
            }

            // 投递到StripedExecutor
            InstanceKey instanceKey = InstanceKey.of(instance.getStrategyId(), instance.getTradingPairId());
            Runnable task = taskFactory.createDecisionTask(instance, event);
            stripedExecutor.submit(instanceKey, task);

            log.debug("价格阈值穿越事件已路由: strategyId={}, tradingPairId={}, triggerType={}",
                event.getStrategyId(), event.getTradingPairId(), event.getTriggerType());

        } catch (Exception e) {
            log.error("处理价格阈值穿越事件失败: strategyId={}, tradingPairId={}",
                event.getStrategyId(), event.getTradingPairId(), e);
            // 不抛异常，允许继续处理其他事件
        }
    }
}

