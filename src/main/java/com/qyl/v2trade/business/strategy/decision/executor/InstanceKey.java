package com.qyl.v2trade.business.strategy.decision.executor;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 策略实例键（用于StripedExecutor的hash分片）
 * 
 * <p>一个策略 + 一个交易对 = 一个实例键
 * 
 * <p>用于保证同一StrategyInstance的决策串行执行
 */
@Getter
@EqualsAndHashCode
public class InstanceKey implements Serializable {

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
     * 构造函数
     * 
     * @param strategyId 策略ID
     * @param tradingPairId 交易对ID
     */
    public InstanceKey(Long strategyId, Long tradingPairId) {
        if (strategyId == null || strategyId <= 0) {
            throw new IllegalArgumentException("策略ID无效: " + strategyId);
        }
        if (tradingPairId == null || tradingPairId <= 0) {
            throw new IllegalArgumentException("交易对ID无效: " + tradingPairId);
        }
        this.strategyId = strategyId;
        this.tradingPairId = tradingPairId;
    }

    /**
     * 创建实例键
     * 
     * @param strategyId 策略ID
     * @param tradingPairId 交易对ID
     * @return 实例键
     */
    public static InstanceKey of(Long strategyId, Long tradingPairId) {
        return new InstanceKey(strategyId, tradingPairId);
    }

    @Override
    public String toString() {
        return "InstanceKey{" +
                "strategyId=" + strategyId +
                ", tradingPairId=" + tradingPairId +
                '}';
    }
}

