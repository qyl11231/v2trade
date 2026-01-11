package com.qyl.v2trade.business.strategy.decision.event;

import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 价格阈值穿越事件
 * 
 * <p>当价格穿越止盈止损阈值或突破价位时发布此事件
 * 
 * <p>用于触发策略决策（实时风控触发器）
 * 
 * <p>路由规则：
 * <ul>
 *   <li>根据strategyId + tradingPairId直接定位StrategyInstance</li>
 *   <li>如果找不到，记录WARN日志（可能是策略已停止）</li>
 * </ul>
 * 
 * <p>触发类型：
 * <ul>
 *   <li>TAKE_PROFIT: 止盈</li>
 *   <li>STOP_LOSS: 止损</li>
 *   <li>BREAKOUT: 突破价位</li>
 * </ul>
 */
public class PriceTriggeredDecisionEvent extends ApplicationEvent {

    /**
     * 策略ID
     */
    private final Long strategyId;

    /**
     * 交易对ID
     */
    private final Long tradingPairId;

    /**
     * 触发类型：TAKE_PROFIT / STOP_LOSS / BREAKOUT
     */
    private final String triggerType;

    /**
     * 触发价格（阈值）
     */
    private final BigDecimal triggerPrice;

    /**
     * 当前价格（穿越时的价格）
     */
    private final BigDecimal currentPrice;

    /**
     * 触发时间
     */
    private final LocalDateTime triggeredAt;

    /**
     * 构造函数
     * 
     * @param source 事件源
     * @param strategyId 策略ID
     * @param tradingPairId 交易对ID
     * @param triggerType 触发类型
     * @param triggerPrice 触发价格
     * @param currentPrice 当前价格
     * @param triggeredAt 触发时间
     */
    public PriceTriggeredDecisionEvent(Object source, Long strategyId, Long tradingPairId,
                                       String triggerType, BigDecimal triggerPrice,
                                       BigDecimal currentPrice, LocalDateTime triggeredAt) {
        super(source);
        this.strategyId = strategyId;
        this.tradingPairId = tradingPairId;
        this.triggerType = triggerType;
        this.triggerPrice = triggerPrice;
        this.currentPrice = currentPrice;
        this.triggeredAt = triggeredAt;
    }

    public Long getStrategyId() {
        return strategyId;
    }

    public Long getTradingPairId() {
        return tradingPairId;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public BigDecimal getTriggerPrice() {
        return triggerPrice;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public LocalDateTime getTriggeredAt() {
        return triggeredAt;
    }

    @Override
    public String toString() {
        return "PriceTriggeredDecisionEvent{" +
                "strategyId=" + strategyId +
                ", tradingPairId=" + tradingPairId +
                ", triggerType='" + triggerType + '\'' +
                ", triggerPrice=" + triggerPrice +
                ", currentPrice=" + currentPrice +
                ", triggeredAt=" + triggeredAt +
                '}';
    }
}

