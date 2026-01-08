package com.qyl.v2trade.business.strategy.loader;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qyl.v2trade.business.strategy.loader.model.StrategyConfigSnapshot;
import com.qyl.v2trade.business.strategy.mapper.StrategyDefinitionMapper;
import com.qyl.v2trade.business.strategy.mapper.StrategyParamMapper;
import com.qyl.v2trade.business.strategy.mapper.StrategySignalSubscriptionMapper;
import com.qyl.v2trade.business.strategy.mapper.StrategySymbolMapper;
import com.qyl.v2trade.business.strategy.model.entity.StrategyDefinition;
import com.qyl.v2trade.business.strategy.model.entity.StrategyParam;
import com.qyl.v2trade.business.strategy.model.entity.StrategySignalSubscription;
import com.qyl.v2trade.business.strategy.model.entity.StrategySymbol;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 策略配置加载器
 * 
 * <p>职责：
 * <ul>
 *   <li>从数据库加载策略配置（definition、param、symbol、subscription）</li>
 *   <li>转换为不可变的内存快照对象</li>
 *   <li>不修改数据，不创建实例</li>
 * </ul>
 * 
 * <p>阶段1约束：
 * <ul>
 *   <li>只读操作，不修改数据库</li>
 *   <li>不消费信号，不生成决策</li>
 * </ul>
 * 
 * @author 开发团队
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StrategyConfigLoader {

    private final StrategyDefinitionMapper definitionMapper;
    private final StrategyParamMapper paramMapper;
    private final StrategySymbolMapper symbolMapper;
    private final StrategySignalSubscriptionMapper subscriptionMapper;

    /**
     * 加载策略配置快照
     * 
     * <p>加载流程：
     * <ol>
     *   <li>加载 strategy_definition（必须存在且启用）</li>
     *   <li>加载 strategy_param（必须存在）</li>
     *   <li>加载 strategy_symbol（只加载enabled=1的）</li>
     *   <li>加载 strategy_signal_subscription（只加载enabled=1的）</li>
     *   <li>组装成 StrategyConfigSnapshot</li>
     * </ol>
     * 
     * @param strategyId 策略ID
     * @return 策略配置快照，如果策略不存在、未启用或配置不完整返回null
     * @throws IllegalArgumentException 如果strategyId为null或<=0
     */
    @Transactional(readOnly = true)
    public StrategyConfigSnapshot loadConfig(Long strategyId) {
        // 参数校验
        if (strategyId == null || strategyId <= 0) {
            throw new IllegalArgumentException(
                String.format("策略ID无效: %s，必须大于0", strategyId)
            );
        }

        log.debug("开始加载策略配置: strategyId={}", strategyId);

        try {
            // 1. 加载策略定义（必须存在且启用）
            StrategyDefinition definition = definitionMapper.selectById(strategyId);
            if (definition == null) {
                log.warn("策略定义不存在: strategyId={}", strategyId);
                return null;
            }

            if (definition.getEnabled() == null || definition.getEnabled() != 1) {
                log.debug("策略未启用: strategyId={}", strategyId);
                return null;
            }

            // 2. 加载策略参数（必须存在）
            StrategyParam param = paramMapper.selectOne(
                new LambdaQueryWrapper<StrategyParam>()
                    .eq(StrategyParam::getStrategyId, strategyId)
            );
            if (param == null) {
                log.warn("策略参数不存在: strategyId={}", strategyId);
                return null;
            }

            // 3. 加载交易对（只加载enabled=1的）
            List<StrategySymbol> allSymbols = symbolMapper.selectList(
                new LambdaQueryWrapper<StrategySymbol>()
                    .eq(StrategySymbol::getStrategyId, strategyId)
            );
            List<StrategySymbol> enabledSymbols = allSymbols.stream()
                .filter(s -> s.getEnabled() != null && s.getEnabled() == 1)
                .collect(Collectors.toList());

            if (enabledSymbols.isEmpty()) {
                log.warn("策略没有启用的交易对: strategyId={}", strategyId);
                return null;
            }

            // 4. 加载信号订阅（只加载enabled=1的）
            List<StrategySignalSubscription> allSubscriptions = subscriptionMapper.selectList(
                new LambdaQueryWrapper<StrategySignalSubscription>()
                    .eq(StrategySignalSubscription::getStrategyId, strategyId)
            );
            List<StrategySignalSubscription> enabledSubscriptions = allSubscriptions.stream()
                .filter(s -> s.getEnabled() != null && s.getEnabled() == 1)
                .collect(Collectors.toList());

            // 5. 组装成快照
            StrategyConfigSnapshot snapshot = StrategyConfigSnapshot.builder()
                .definition(definition)
                .param(param)
                .symbols(enabledSymbols)
                .subscriptions(enabledSubscriptions)
                .build();

            log.info("策略配置加载成功: strategyId={}, symbolCount={}, subscriptionCount={}",
                strategyId, snapshot.getSymbolCount(), snapshot.getSubscriptionCount());

            return snapshot;

        } catch (Exception e) {
            log.error("加载策略配置失败: strategyId={}", strategyId, e);
            throw new RuntimeException("加载策略配置失败: strategyId=" + strategyId, e);
        }
    }
}

