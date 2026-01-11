package com.qyl.v2trade.business.strategy.decision.logic;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 策略逻辑注册表
 * 
 * <p>职责：
 * <ul>
 *   <li>管理所有策略逻辑实现</li>
 *   <li>按策略类型路由到对应的实现</li>
 *   <li>支持动态注册</li>
 * </ul>
 * 
 * <p>阶段2约束：
 * <ul>
 *   <li>找不到策略逻辑实现时返回默认实现（HOLD）</li>
 *   <li>支持SIGNAL_DRIVEN / INDICATOR_DRIVEN / HYBRID三种类型</li>
 * </ul>
 */
@Slf4j
@Component
public class StrategyLogicRegistry {

    /**
     * 策略逻辑实现映射
     * Key: 策略类型（SIGNAL_DRIVEN / INDICATOR_DRIVEN / HYBRID）
     * Value: 策略逻辑实现
     */
    private final Map<String, StrategyLogic> logicMap = new HashMap<>();

    /**
     * 所有策略逻辑实现（由Spring自动注入）
     */
    @Autowired(required = false)
    private List<StrategyLogic> allLogics;

    /**
     * 初始化注册表
     * 
     * <p>扫描所有StrategyLogic实现，按支持的策略类型注册
     */
    @PostConstruct
    public void init() {
        if (allLogics != null) {
            for (StrategyLogic logic : allLogics) {
                String strategyType = logic.getSupportedStrategyType();
                if (strategyType != null && !strategyType.isEmpty()) {
                    StrategyLogic existing = logicMap.put(strategyType, logic);
                    if (existing != null) {
                        log.warn("策略逻辑实现重复注册，将被覆盖: strategyType={}, existing={}, new={}",
                            strategyType, existing.getClass().getName(), logic.getClass().getName());
                    } else {
                        log.info("策略逻辑实现注册成功: strategyType={}, implementation={}",
                            strategyType, logic.getClass().getName());
                    }
                } else {
                    log.warn("策略逻辑实现未指定支持的策略类型，跳过注册: {}", logic.getClass().getName());
                }
            }
        }

        log.info("策略逻辑注册表初始化完成: registeredTypes={}", logicMap.keySet());
    }

    /**
     * 根据策略类型获取策略逻辑实现
     * 
     * @param strategyType 策略类型
     * @return 策略逻辑实现，如果不存在返回默认实现（HOLD）
     */
    public StrategyLogic getLogic(String strategyType) {
        if (strategyType == null || strategyType.isEmpty()) {
            log.warn("策略类型为空，返回默认实现");
            return getDefaultLogic();
        }

        StrategyLogic logic = logicMap.get(strategyType);
        if (logic == null) {
            log.warn("未找到策略逻辑实现: strategyType={}, 返回默认实现", strategyType);
            return getDefaultLogic();
        }

        return logic;
    }

    /**
     * 注册策略逻辑实现（支持动态注册）
     * 
     * @param logic 策略逻辑实现
     */
    public void register(StrategyLogic logic) {
        if (logic == null) {
            throw new IllegalArgumentException("策略逻辑实现不能为null");
        }

        String strategyType = logic.getSupportedStrategyType();
        if (strategyType == null || strategyType.isEmpty()) {
            throw new IllegalArgumentException("策略逻辑实现必须指定支持的策略类型");
        }

        StrategyLogic existing = logicMap.put(strategyType, logic);
        if (existing != null) {
            log.warn("策略逻辑实现已存在，将被替换: strategyType={}, existing={}, new={}",
                strategyType, existing.getClass().getName(), logic.getClass().getName());
        } else {
            log.info("策略逻辑实现注册成功: strategyType={}, implementation={}",
                strategyType, logic.getClass().getName());
        }
    }

    /**
     * 获取默认策略逻辑实现（返回HOLD）
     * 
     * @return 默认策略逻辑实现
     */
    private StrategyLogic getDefaultLogic() {
        return new DefaultStrategyLogic();
    }

    /**
     * 默认策略逻辑实现（返回HOLD）
     */
    private static class DefaultStrategyLogic implements StrategyLogic {
        @Override
        public DecisionResult decide(com.qyl.v2trade.business.strategy.decision.context.DecisionContext ctx) {
            return DecisionResult.hold(DecisionReason.builder()
                .triggerType(ctx.getTriggerType() != null ? ctx.getTriggerType().getCode() : "UNKNOWN")
                .triggerSource("DEFAULT")
                .triggerTimestamp(java.time.LocalDateTime.now().toString())
                .decisionBasis(Map.of("reason", "未找到策略逻辑实现"))
                .stateChange("保持当前状态")
                .build());
        }

        @Override
        public String getSupportedStrategyType() {
            return "DEFAULT";
        }
    }
}

