package com.qyl.v2trade.business.strategy.decision.context;

import com.qyl.v2trade.business.strategy.decision.context.snapshot.*;
import com.qyl.v2trade.common.constants.DecisionTriggerTypeEnum;
import lombok.Builder;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 决策上下文（不可变）
 * 
 * <p>职责：
 * <ul>
 *   <li>包含决策时点的所有数据快照</li>
 *   <li>不可变对象，保证决策过程的数据一致性</li>
 *   <li>由AtomicSampler在同一时间点采集</li>
 * </ul>
 * 
 * <p>阶段2约束：
 * <ul>
 *   <li>所有数据都是只读快照</li>
 *   <li>不包含数据库连接或可变的业务对象</li>
 *   <li>用于纯函数式的策略逻辑计算</li>
 * </ul>
 */
@Getter
@Builder
public class DecisionContext implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private final Long userId;

    /**
     * 策略ID
     */
    private final Long strategyId;

    /**
     * 交易对ID
     */
    private final Long tradingPairId;

    /**
     * 策略类型：SIGNAL_DRIVEN / INDICATOR_DRIVEN / HYBRID
     */
    private final String strategyType;

    /**
     * 触发类型：SIGNAL / INDICATOR / BAR / PRICE
     */
    private final DecisionTriggerTypeEnum triggerType;

    /**
     * 触发时间
     */
    private final LocalDateTime triggeredAt;

    /**
     * 逻辑状态快照（决策前的状态）
     */
    private final LogicStateSnapshot logicStateBefore;

    /**
     * 策略参数快照
     */
    private final ParamSnapshot paramSnapshot;

    /**
     * 信号快照（可空，LATEST_ONLY）
     */
    private final SignalSnapshot signalSnapshot;

    /**
     * 指标快照（可空，最新值）
     */
    private final IndicatorSnapshot indicatorSnapshot;

    /**
     * K线快照（可空，如果由BarClosedEvent触发）
     */
    private final BarSnapshot barSnapshot;

    /**
     * 价格快照（可空，如果价格服务未就绪）
     */
    private final PriceSnapshot priceSnapshot;

    /**
     * 触发事件原始信息（用于构建decision_reason）
     */
    private final Object triggerEvent;

    /**
     * 判断是否有信号数据
     */
    public boolean hasSignal() {
        return signalSnapshot != null && signalSnapshot.isValid();
    }

    /**
     * 判断是否有指标数据
     */
    public boolean hasIndicator() {
        return indicatorSnapshot != null && indicatorSnapshot.hasValue();
    }

    /**
     * 判断是否有K线数据
     */
    public boolean hasBar() {
        return barSnapshot != null;
    }

    /**
     * 判断是否有价格数据
     */
    public boolean hasPrice() {
        return priceSnapshot != null && priceSnapshot.isAvailable();
    }

    /**
     * 判断是否为空仓状态
     */
    public boolean isFlat() {
        return logicStateBefore != null && logicStateBefore.isFlat();
    }

    /**
     * 判断是否持仓状态
     */
    public boolean hasPosition() {
        return logicStateBefore != null && logicStateBefore.hasPosition();
    }
}

