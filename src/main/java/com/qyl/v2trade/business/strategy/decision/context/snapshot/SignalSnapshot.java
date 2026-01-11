package com.qyl.v2trade.business.strategy.decision.context.snapshot;

import lombok.Builder;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 信号快照（不可变）
 * 
 * <p>从 signal_intent 表读取的信号快照（LATEST_ONLY）
 * 
 * <p>用于信号驱动策略的决策
 */
@Getter
@Builder
public class SignalSnapshot implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
     * 意图状态：ACTIVE / CONSUMED / EXPIRED / IGNORED
     */
    private final String intentStatus;

    /**
     * 信号产生时间
     */
    private final LocalDateTime generatedAt;

    /**
     * 系统接收时间
     */
    private final LocalDateTime receivedAt;

    /**
     * 失效时间（可空）
     */
    private final LocalDateTime expiredAt;

    /**
     * 判断信号是否有效（ACTIVE状态）
     */
    public boolean isActive() {
        return "ACTIVE".equals(intentStatus);
    }

    /**
     * 判断信号是否已过期
     */
    public boolean isExpired() {
        if (expiredAt == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(expiredAt);
    }

    /**
     * 判断信号是否有效且未过期
     */
    public boolean isValid() {
        return isActive() && !isExpired();
    }
}

