package com.qyl.v2trade.business.strategy.factory;

import com.qyl.v2trade.business.strategy.factory.model.StrategyInstance;
import com.qyl.v2trade.business.strategy.factory.model.StrategyRuntime;
import com.qyl.v2trade.business.strategy.loader.model.StrategyConfigSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 策略运行时工厂
 * 
 * <p>职责：
 * <ul>
 *   <li>将配置快照转换为策略运行时</li>
 *   <li>一个策略 → N个实例（按交易对拆分）</li>
 *   <li>注入共享参数到每个实例</li>
 *   <li>不访问数据库，纯内存操作</li>
 * </ul>
 * 
 * <p>阶段1约束：
 * <ul>
 *   <li>不访问数据库</li>
 *   <li>不恢复状态（状态恢复由LogicStateRestorer负责）</li>
 *   <li>不绑定信号视图（信号视图绑定由SignalViewBinder负责）</li>
 * </ul>
 * 
 * @author 开发团队
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StrategyRuntimeFactory {

    /**
     * 创建策略运行时
     * 
     * <p>创建流程：
     * <ol>
     *   <li>创建 StrategyRuntime 对象</li>
     *   <li>为每个交易对创建 StrategyInstance</li>
     *   <li>注入共享参数（通过StrategyRuntime）</li>
     *   <li>返回运行时对象</li>
     * </ol>
     * 
     * <p>注意：实例的logicState和signalViews需要在后续步骤中设置
     * 
     * @param config 策略配置快照
     * @return 策略运行时
     * @throws IllegalArgumentException 如果config为null或不完整
     */
    public StrategyRuntime createRuntime(StrategyConfigSnapshot config) {
        // 参数校验
        if (config == null) {
            throw new IllegalArgumentException("策略配置快照不能为null");
        }

        if (!config.isComplete()) {
            throw new IllegalArgumentException(
                String.format("策略配置不完整: strategyId=%s", config.getStrategyId())
            );
        }

        log.debug("开始创建策略运行时: strategyId={}, symbolCount={}",
            config.getStrategyId(), config.getSymbolCount());

        try {
            // 1. 创建实例映射
            Map<Long, StrategyInstance> instances = new HashMap<>();

            // 2. 为每个交易对创建实例
            for (var symbol : config.getSymbols()) {
                StrategyInstance instance = StrategyInstance.builder()
                    .strategyId(config.getStrategyId())
                    .tradingPairId(symbol.getTradingPairId())
                    .build();

                instances.put(symbol.getTradingPairId(), instance);

                log.debug("创建策略实例: strategyId={}, tradingPairId={}",
                    config.getStrategyId(), symbol.getTradingPairId());
            }

            // 3. 创建运行时对象
            StrategyRuntime runtime = StrategyRuntime.builder()
                .strategyId(config.getStrategyId())
                .decisionMode(config.getDecisionMode())
                .param(config.getParam())
                .instances(instances)
                .build();

            log.info("策略运行时创建成功: strategyId={}, instanceCount={}",
                config.getStrategyId(), runtime.getInstanceCount());

            return runtime;

        } catch (Exception e) {
            log.error("创建策略运行时失败: strategyId={}", config.getStrategyId(), e);
            throw new RuntimeException(
                "创建策略运行时失败: strategyId=" + config.getStrategyId(), e
            );
        }
    }
}

