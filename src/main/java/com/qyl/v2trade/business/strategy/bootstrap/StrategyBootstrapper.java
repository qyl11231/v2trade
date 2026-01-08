package com.qyl.v2trade.business.strategy.bootstrap;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qyl.v2trade.business.strategy.binder.SignalViewBinder;
import com.qyl.v2trade.business.strategy.factory.StrategyRuntimeFactory;
import com.qyl.v2trade.business.strategy.factory.model.StrategyInstance;
import com.qyl.v2trade.business.strategy.factory.model.StrategyRuntime;
import com.qyl.v2trade.business.strategy.loader.StrategyConfigLoader;
import com.qyl.v2trade.business.strategy.loader.model.StrategyConfigSnapshot;
import com.qyl.v2trade.business.strategy.mapper.StrategyDefinitionMapper;
import com.qyl.v2trade.business.strategy.model.entity.StrategyDefinition;
import com.qyl.v2trade.business.strategy.restorer.LogicStateRestorer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 策略启动协调器
 * 
 * <p>职责：
 * <ul>
 *   <li>协调整个策略实例化过程</li>
 *   <li>在系统启动/策略启停时触发</li>
 *   <li>调度所有Loader、Factory、Restorer、Binder</li>
 *   <li>维护 StrategyRuntimeRegistry</li>
 * </ul>
 * 
 * <p>阶段1约束：
 * <ul>
 *   <li>不直接操作数据库，不包含业务逻辑</li>
 *   <li>部分失败不影响其他策略</li>
 *   <li>异常处理完善，记录日志</li>
 * </ul>
 * 
 * @author 开发团队
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StrategyBootstrapper {

    private final StrategyDefinitionMapper definitionMapper;
    private final StrategyConfigLoader configLoader;
    private final StrategyRuntimeFactory runtimeFactory;
    private final LogicStateRestorer stateRestorer;
    private final SignalViewBinder signalViewBinder;
    private final StrategyRuntimeRegistry registry;

    /**
     * 启动所有启用的策略
     * 
     * <p>启动流程：
     * <ol>
     *   <li>查询所有启用的策略</li>
     *   <li>为每个策略调用 bootstrapStrategy</li>
     *   <li>部分失败不影响其他策略</li>
     * </ol>
     */
    public void bootstrapAll() {
        log.info("开始启动所有启用策略");

        try {
            // 1. 查询所有启用的策略
            List<StrategyDefinition> enabledStrategies = definitionMapper.selectList(
                new LambdaQueryWrapper<StrategyDefinition>()
                    .eq(StrategyDefinition::getEnabled, 1)
            );

            log.info("找到 {} 个启用的策略", enabledStrategies.size());

            // 2. 为每个策略启动
            int successCount = 0;
            int failCount = 0;

            for (StrategyDefinition strategy : enabledStrategies) {
                try {
                    bootstrapStrategy(strategy.getId());
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    log.error("策略启动失败: strategyId={}", strategy.getId(), e);
                    // 不抛出异常，继续处理其他策略
                }
            }

            log.info("策略启动完成: 总数={}, 成功={}, 失败={}",
                enabledStrategies.size(), successCount, failCount);

        } catch (Exception e) {
            log.error("启动所有策略失败", e);
            // 不抛出异常，允许系统继续启动
        }
    }

    /**
     * 启动单个策略
     * 
     * <p>启动流程：
     * <ol>
     *   <li>加载策略配置</li>
     *   <li>创建策略运行时</li>
     *   <li>为每个实例恢复状态</li>
     *   <li>绑定信号视图</li>
     *   <li>注册到注册表</li>
     * </ol>
     * 
     * @param strategyId 策略ID
     * @throws IllegalArgumentException 如果strategyId无效
     */
    public void bootstrapStrategy(Long strategyId) {
        if (strategyId == null || strategyId <= 0) {
            throw new IllegalArgumentException(
                String.format("策略ID无效: %s，必须大于0", strategyId)
            );
        }

        log.info("开始启动策略: strategyId={}", strategyId);

        try {
            // 1. 加载配置
            StrategyConfigSnapshot config = configLoader.loadConfig(strategyId);
            if (config == null) {
                log.warn("策略配置不存在或未启用: strategyId={}", strategyId);
                return;
            }

            // 2. 创建运行时
            StrategyRuntime runtime = runtimeFactory.createRuntime(config);

            // 3. 为每个实例恢复状态并绑定信号视图
            Long userId = config.getUserId();
            for (StrategyInstance instance : runtime.getAllInstances()) {
                // 3.1 恢复状态
                instance.setLogicState(
                    stateRestorer.restoreOrInit(
                        userId,
                        strategyId,
                        instance.getTradingPairId()
                    )
                );

                // 3.2 绑定信号视图
                signalViewBinder.bindSignalViews(instance, config.getSubscriptions());
            }

            // 4. 注册到注册表
            registry.register(runtime);

            log.info("策略启动成功: strategyId={}, instanceCount={}",
                strategyId, runtime.getInstanceCount());

        } catch (Exception e) {
            log.error("策略启动失败: strategyId={}", strategyId, e);
            // 不抛出异常，允许继续处理其他策略
            throw new RuntimeException("策略启动失败: strategyId=" + strategyId, e);
        }
    }

    /**
     * 停止策略
     * 
     * <p>停止流程：
     * <ol>
     *   <li>从注册表注销运行时</li>
     *   <li>清理内存资源</li>
     * </ol>
     * 
     * @param strategyId 策略ID
     */
    public void stopStrategy(Long strategyId) {
        if (strategyId == null || strategyId <= 0) {
            log.warn("策略ID无效，跳过停止: strategyId={}", strategyId);
            return;
        }

        log.info("开始停止策略: strategyId={}", strategyId);

        try {
            StrategyRuntime removed = registry.unregister(strategyId);
            if (removed != null) {
                log.info("策略停止成功: strategyId={}", strategyId);
            } else {
                log.warn("策略运行时不存在，无需停止: strategyId={}", strategyId);
            }

        } catch (Exception e) {
            log.error("策略停止失败: strategyId={}", strategyId, e);
            // 不抛出异常，允许继续处理
        }
    }
}

