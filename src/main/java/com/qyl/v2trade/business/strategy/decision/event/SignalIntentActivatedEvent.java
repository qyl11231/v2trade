package com.qyl.v2trade.business.strategy.decision.event;

import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * 信号意图激活事件
 * 
 * <p>当signal_intent表中出现新的ACTIVE记录时发布此事件
 * 
 * <p>用于触发策略决策（信号驱动策略）
 * 
 * <p>路由规则：
 * <ul>
 *   <li>根据strategyId + tradingPairId查找StrategyInstance</li>
 *   <li>如果找不到，记录WARN日志，不抛异常</li>
 * </ul>
 */
public class SignalIntentActivatedEvent extends ApplicationEvent {

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
     * 信号意图ID（signal_intent.id）
     */
    private final Long signalIntentId;

    /**
     * 信号ID（signal_intent.signal_id，如TradingView alert id）
     */
    private final String signalId;

    /**
     * 意图方向：BUY / SELL / FLAT / REVERSE
     */
    private final String intentDirection;

    /**
     * 激活时间（signal_intent.received_at）
     */
    private final LocalDateTime activatedAt;

    /**
     * 构造函数
     * 
     * @param source 事件源
     * @param userId 用户ID
     * @param strategyId 策略ID
     * @param tradingPairId 交易对ID
     * @param signalIntentId 信号意图ID
     * @param signalId 信号ID
     * @param intentDirection 意图方向
     * @param activatedAt 激活时间
     */
    public SignalIntentActivatedEvent(Object source, Long userId, Long strategyId, 
                                     Long tradingPairId, Long signalIntentId, 
                                     String signalId, String intentDirection, 
                                     LocalDateTime activatedAt) {
        super(source);
        this.userId = userId;
        this.strategyId = strategyId;
        this.tradingPairId = tradingPairId;
        this.signalIntentId = signalIntentId;
        this.signalId = signalId;
        this.intentDirection = intentDirection;
        this.activatedAt = activatedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getStrategyId() {
        return strategyId;
    }

    public Long getTradingPairId() {
        return tradingPairId;
    }

    public Long getSignalIntentId() {
        return signalIntentId;
    }

    public String getSignalId() {
        return signalId;
    }

    public String getIntentDirection() {
        return intentDirection;
    }

    public LocalDateTime getActivatedAt() {
        return activatedAt;
    }

    @Override
    public String toString() {
        return "SignalIntentActivatedEvent{" +
                "userId=" + userId +
                ", strategyId=" + strategyId +
                ", tradingPairId=" + tradingPairId +
                ", signalIntentId=" + signalIntentId +
                ", signalId='" + signalId + '\'' +
                ", intentDirection='" + intentDirection + '\'' +
                ", activatedAt=" + activatedAt +
                '}';
    }
}

