package com.qyl.v2trade.business.strategy.decision.logic.condition;

import lombok.Builder;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 命中原因（用于审计）
 * 
 * <p>包含完整的规则和组命中情况，便于回放和审计
 */
@Getter
@Builder
public class HitReason implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 规则命中情况
     */
    private final List<RuleHit> ruleHits;

    /**
     * 组命中情况
     */
    private final List<GroupHit> groupHits;

    /**
     * 规则命中
     */
    @Getter
    @Builder
    public static class RuleHit implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 因子key
         */
        private final String factor;

        /**
         * 操作符
         */
        private final String operator;

        /**
         * 比较值
         */
        private final Object value;

        /**
         * 是否命中
         */
        private final boolean hit;

        /**
         * 命中/未命中原因
         */
        private final String reason;
    }

    /**
     * 组命中
     */
    @Getter
    @Builder
    public static class GroupHit implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 组模式（ALL/ANY）
         */
        private final String mode;

        /**
         * 是否命中
         */
        private final boolean hit;

        /**
         * 组内规则命中情况
         */
        private final List<RuleHit> ruleHits;
    }
}

