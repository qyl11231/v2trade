package com.qyl.v2trade.business.strategy.loader.model;

import com.qyl.v2trade.business.strategy.model.entity.StrategyDefinition;
import com.qyl.v2trade.business.strategy.model.entity.StrategyParam;
import com.qyl.v2trade.business.strategy.model.entity.StrategySignalSubscription;
import com.qyl.v2trade.business.strategy.model.entity.StrategySymbol;
import lombok.Builder;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 策略配置快照（不可变对象）
 * 
 * <p>职责：
 * <ul>
 *   <li>在内存中保存策略的完整配置信息</li>
 *   <li>作为配置加载器的输出，供运行时工厂使用</li>
 *   <li>不可变设计，确保配置在实例化过程中不被修改</li>
 * </ul>
 * 
 * <p>阶段1约束：
 * <ul>
 *   <li>只读快照，不修改数据</li>
 *   <li>不包含状态信息，只包含配置信息</li>
 * </ul>
 */
@Getter
@Builder
public class StrategyConfigSnapshot implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 策略定义
     */
    private final StrategyDefinition definition;

    /**
     * 策略参数
     */
    private final StrategyParam param;

    /**
     * 交易对列表（只包含启用的）
     */
    private final List<StrategySymbol> symbols;

    /**
     * 信号订阅列表（只包含启用的）
     */
    private final List<StrategySignalSubscription> subscriptions;

    /**
     * 获取策略ID
     * 
     * @return 策略ID
     */
    public Long getStrategyId() {
        return definition != null ? definition.getId() : null;
    }

    /**
     * 获取用户ID
     * 
     * @return 用户ID
     */
    public Long getUserId() {
        return definition != null ? definition.getUserId() : null;
    }

    /**
     * 获取策略名称
     * 
     * @return 策略名称
     */
    public String getStrategyName() {
        return definition != null ? definition.getStrategyName() : null;
    }

    /**
     * 获取策略类型
     * 
     * @return 策略类型
     */
    public String getStrategyType() {
        return definition != null ? definition.getStrategyType() : null;
    }

    /**
     * 获取决策模式
     * 
     * @return 决策模式
     */
    public String getDecisionMode() {
        return definition != null ? definition.getDecisionMode() : null;
    }

    /**
     * 获取启用的交易对数量
     * 
     * @return 交易对数量
     */
    public int getSymbolCount() {
        return symbols != null ? symbols.size() : 0;
    }

    /**
     * 获取启用的信号订阅数量
     * 
     * @return 信号订阅数量
     */
    public int getSubscriptionCount() {
        return subscriptions != null ? subscriptions.size() : 0;
    }

    /**
     * 获取交易对列表（不可变）
     * 
     * @return 交易对列表
     */
    public List<StrategySymbol> getSymbols() {
        return symbols != null ? Collections.unmodifiableList(symbols) : Collections.emptyList();
    }

    /**
     * 获取信号订阅列表（不可变）
     * 
     * @return 信号订阅列表
     */
    public List<StrategySignalSubscription> getSubscriptions() {
        return subscriptions != null ? Collections.unmodifiableList(subscriptions) : Collections.emptyList();
    }

    /**
     * 检查配置是否完整
     * 
     * @return true如果配置完整，false否则
     */
    public boolean isComplete() {
        return definition != null 
            && param != null 
            && symbols != null 
            && !symbols.isEmpty();
    }
}

