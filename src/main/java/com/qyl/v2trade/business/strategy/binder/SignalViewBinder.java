package com.qyl.v2trade.business.strategy.binder;

import com.qyl.v2trade.business.strategy.binder.model.SignalView;
import com.qyl.v2trade.business.strategy.factory.model.StrategyInstance;
import com.qyl.v2trade.business.strategy.model.entity.StrategySignalSubscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 信号观察绑定器
 * 
 * <p>职责：
 * <ul>
 *   <li>为策略实例建立信号观察关系</li>
 *   <li>只建立观察关系，不消费信号</li>
 *   <li>不修改信号状态</li>
 * </ul>
 * 
 * <p>阶段1约束：
 * <ul>
 *   <li>只建立观察关系，不消费信号</li>
 *   <li>不调用 SignalService.consume()</li>
 *   <li>不修改 signal_intent 状态</li>
 *   <li>信号视图标记为只读</li>
 * </ul>
 * 
 * @author 开发团队
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SignalViewBinder {

    /**
     * 绑定信号视图到策略实例
     * 
     * <p>绑定流程：
     * <ol>
     *   <li>为每个subscription创建SignalView</li>
     *   <li>设置consumeMode</li>
     *   <li>标记为只读</li>
     *   <li>绑定到instance</li>
     * </ol>
     * 
     * <p>注意：不调用SignalService，不消费信号
     * 
     * @param instance 策略实例
     * @param subscriptions 信号订阅列表
     * @throws IllegalArgumentException 如果参数无效
     */
    public void bindSignalViews(StrategyInstance instance, 
                                List<StrategySignalSubscription> subscriptions) {
        // 参数校验
        if (instance == null) {
            throw new IllegalArgumentException("策略实例不能为null");
        }

        if (subscriptions == null) {
            log.debug("信号订阅列表为空，跳过绑定: strategyId={}, tradingPairId={}",
                instance.getStrategyId(), instance.getTradingPairId());
            return;
        }

        log.debug("开始绑定信号视图: strategyId={}, tradingPairId={}, subscriptionCount={}",
            instance.getStrategyId(), instance.getTradingPairId(), subscriptions.size());

        try {
            // 为每个订阅创建信号视图
            for (StrategySignalSubscription subscription : subscriptions) {
                // 阶段1核心约束：只建立观察关系，不消费信号
                // 不调用 SignalService.consume() 或修改 signal_intent 状态
                
                SignalView signalView = SignalView.builder()
                    .signalConfigId(subscription.getSignalConfigId())
                    .consumeMode(subscription.getConsumeMode())
                    .readOnly(true) // 标记为只读
                    .build();

                instance.addSignalView(signalView);

                log.debug("信号视图绑定成功: strategyId={}, tradingPairId={}, signalConfigId={}, consumeMode={}",
                    instance.getStrategyId(), instance.getTradingPairId(),
                    subscription.getSignalConfigId(), subscription.getConsumeMode());
            }

            log.info("信号视图绑定完成: strategyId={}, tradingPairId={}, viewCount={}",
                instance.getStrategyId(), instance.getTradingPairId(), instance.getSignalViewCount());

        } catch (Exception e) {
            log.error("绑定信号视图失败: strategyId={}, tradingPairId={}",
                instance.getStrategyId(), instance.getTradingPairId(), e);
            throw new RuntimeException(
                String.format("绑定信号视图失败: strategyId=%s, tradingPairId=%s",
                    instance.getStrategyId(), instance.getTradingPairId()), e
            );
        }
    }
}

