package com.qyl.v2trade.business.strategy.factory.model;

import com.qyl.v2trade.business.strategy.binder.model.SignalView;
import com.qyl.v2trade.business.strategy.model.entity.StrategyLogicState;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 策略实例（一个策略 + 一个交易对 = 一个实例）
 * 
 * <p>职责：
 * <ul>
 *   <li>代表策略在某个交易对上的运行实例</li>
 *   <li>包含该实例的逻辑状态和信号视图</li>
 *   <li>每个实例独立管理自己的状态</li>
 * </ul>
 * 
 * <p>实例粒度：
 * <ul>
 *   <li>策略参数：共享（来自StrategyRuntime）</li>
 *   <li>信号订阅：共享（来自StrategyRuntime）</li>
 *   <li>逻辑状态：每个交易对独立</li>
 *   <li>信号视图：每个交易对独立</li>
 * </ul>
 */
@Getter
@Setter
@Builder
public class StrategyInstance implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 策略ID
     */
    private final Long strategyId;

    /**
     * 交易对ID
     */
    private final Long tradingPairId;

    /**
     * 逻辑状态快照（每个实例独立）
     */
    private StrategyLogicState logicState;

    /**
     * 信号视图列表（每个实例独立，只读）
     */
    @Builder.Default
    private List<SignalView> signalViews = new ArrayList<>();

    /**
     * 获取信号视图列表（不可变）
     * 
     * @return 信号视图列表
     */
    public List<SignalView> getSignalViews() {
        return signalViews != null ? Collections.unmodifiableList(signalViews) : Collections.emptyList();
    }

    /**
     * 添加信号视图
     * 
     * @param signalView 信号视图
     */
    public void addSignalView(SignalView signalView) {
        if (signalViews == null) {
            signalViews = new ArrayList<>();
        }
        signalViews.add(signalView);
    }

    /**
     * 获取信号视图数量
     * 
     * @return 信号视图数量
     */
    public int getSignalViewCount() {
        return signalViews != null ? signalViews.size() : 0;
    }
}

