package com.qyl.v2trade.business.strategy.factory.model;

import com.qyl.v2trade.business.strategy.model.entity.StrategyParam;
import lombok.Builder;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 策略运行时
 * 
 * <p>职责：
 * <ul>
 *   <li>代表一个策略的完整运行时环境</li>
 *   <li>包含策略的共享配置和所有交易对实例</li>
 *   <li>管理策略的决策模式和参数</li>
 * </ul>
 * 
 * <p>结构：
 * <ul>
 *   <li>策略ID、决策模式：策略级别</li>
 *   <li>策略参数：所有实例共享</li>
 *   <li>实例列表：每个交易对一个实例</li>
 * </ul>
 */
@Getter
@Builder
public class StrategyRuntime implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 策略ID
     */
    private final Long strategyId;

    /**
     * 决策模式：FOLLOW_SIGNAL / INTENT_DRIVEN
     */
    private final String decisionMode;

    /**
     * 策略参数快照（所有实例共享）
     */
    private final StrategyParam param;

    /**
     * 实例列表（按交易对ID索引）
     */
    private final Map<Long, StrategyInstance> instances;

    /**
     * 获取实例数量
     * 
     * @return 实例数量
     */
    public int getInstanceCount() {
        return instances != null ? instances.size() : 0;
    }

    /**
     * 根据交易对ID获取实例
     * 
     * @param tradingPairId 交易对ID
     * @return 策略实例，如果不存在返回null
     */
    public StrategyInstance getInstance(Long tradingPairId) {
        if (instances == null || tradingPairId == null) {
            return null;
        }
        return instances.get(tradingPairId);
    }

    /**
     * 获取所有实例列表（不可变）
     * 
     * @return 实例列表
     */
    public List<StrategyInstance> getAllInstances() {
        if (instances == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(
            instances.values().stream().collect(Collectors.toList())
        );
    }

    /**
     * 检查是否有实例
     * 
     * @return true如果有实例，false否则
     */
    public boolean hasInstances() {
        return instances != null && !instances.isEmpty();
    }
}

