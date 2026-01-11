package com.qyl.v2trade.business.strategy.decision.router;

import com.qyl.v2trade.business.strategy.bootstrap.StrategyRuntimeRegistry;
import com.qyl.v2trade.business.strategy.factory.model.StrategyInstance;
import com.qyl.v2trade.business.strategy.factory.model.StrategyRuntime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 策略实例定位器
 * 
 * <p>职责：
 * <ul>
 *   <li>根据事件信息查找受影响的策略实例</li>
 *   <li>支持多种查找方式（按strategyId+tradingPairId、按tradingPairId等）</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StrategyInstanceLocator {

    private final StrategyRuntimeRegistry runtimeRegistry;

    /**
     * 根据策略ID和交易对ID查找策略实例
     * 
     * <p>用于SignalIntentActivatedEvent和PriceTriggeredDecisionEvent
     * 
     * @param strategyId 策略ID
     * @param tradingPairId 交易对ID
     * @return 策略实例，如果不存在返回null
     */
    public StrategyInstance locateByStrategyAndPair(Long strategyId, Long tradingPairId) {
        if (strategyId == null || tradingPairId == null) {
            return null;
        }

        StrategyRuntime runtime = runtimeRegistry.getRuntime(strategyId);
        if (runtime == null) {
            log.debug("策略运行时不存在: strategyId={}", strategyId);
            return null;
        }

        StrategyInstance instance = runtime.getInstance(tradingPairId);
        if (instance == null) {
            log.debug("策略实例不存在: strategyId={}, tradingPairId={}", strategyId, tradingPairId);
            return null;
        }

        return instance;
    }

    /**
     * 根据交易对ID查找所有策略实例
     * 
     * <p>用于BarClosedEvent和IndicatorComputedEvent
     * 
     * @param tradingPairId 交易对ID
     * @return 策略实例列表
     */
    public List<StrategyInstance> locateByTradingPair(Long tradingPairId) {
        if (tradingPairId == null) {
            return new ArrayList<>();
        }

        List<StrategyInstance> instances = new ArrayList<>();

        // 遍历所有运行时，查找包含该交易对的实例
        for (StrategyRuntime runtime : runtimeRegistry.getAllRuntimes()) {
            StrategyInstance instance = runtime.getInstance(tradingPairId);
            if (instance != null) {
                instances.add(instance);
            }
        }

        log.debug("找到策略实例: tradingPairId={}, count={}", tradingPairId, instances.size());
        return instances;
    }

    /**
     * 根据交易对ID和指标代码查找订阅该指标的策略实例
     * 
     * <p>用于IndicatorComputedEvent
     * 
     * <p>注意：当前实现返回所有在该交易对上运行的策略实例
     * 后续可以优化为只返回订阅了该指标的策略实例
     * 
     * @param tradingPairId 交易对ID
     * @param indicatorCode 指标代码（可选，用于未来优化）
     * @return 策略实例列表
     */
    public List<StrategyInstance> locateByTradingPairAndIndicator(Long tradingPairId, String indicatorCode) {
        // 当前实现：返回所有在该交易对上运行的策略实例
        // 后续优化：可以查询strategy_signal_subscription表，只返回订阅了该指标的策略
        return locateByTradingPair(tradingPairId);
    }
}

