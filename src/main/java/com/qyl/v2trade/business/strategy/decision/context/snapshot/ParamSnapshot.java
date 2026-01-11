package com.qyl.v2trade.business.strategy.decision.context.snapshot;

import lombok.Builder;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 策略参数快照（不可变）
 * 
 * <p>从 strategy_param 表读取的参数快照
 * 
 * <p>用于决策时的参数计算
 */
@Getter
@Builder
public class ParamSnapshot implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 策略初始虚拟资金
     */
    private final BigDecimal initialCapital;

    /**
     * 单次下单资金占比
     */
    private final BigDecimal baseOrderRatio;

    /**
     * 策略止盈比例（兜底型）
     */
    private final BigDecimal takeProfitRatio;

    /**
     * 策略止损比例（兜底型）
     */
    private final BigDecimal stopLossRatio;

    /**
     * 入场条件（JSON格式）
     */
    private final String entryCondition;

    /**
     * 出场条件（JSON格式）
     */
    private final String exitCondition;

    /**
     * 其他参数（扩展字段，JSON格式）
     */
    private final String extraParams;

    /**
     * 计算下单数量
     * 
     * @return 下单数量 = initialCapital * baseOrderRatio
     */
    public BigDecimal calculateOrderQty() {
        if (initialCapital == null || baseOrderRatio == null) {
            return BigDecimal.ZERO;
        }
        return initialCapital.multiply(baseOrderRatio);
    }
}

