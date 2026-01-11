package com.qyl.v2trade.business.strategy.decision.logic.condition;

import lombok.Builder;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 评估结果
 * 
 * <p>包含：
 * <ul>
 *   <li>passed：是否通过</li>
 *   <li>blocked：是否因缺值被block</li>
 *   <li>blockReason：block原因</li>
 *   <li>hitReason：命中原因（用于审计）</li>
 * </ul>
 */
@Getter
@Builder
public class EvaluationResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 是否通过
     */
    private final boolean passed;

    /**
     * 是否因缺值被block
     */
    private final boolean blocked;

    /**
     * block原因（如果blocked=true）
     */
    private final String blockReason;

    /**
     * 命中原因（用于审计）
     */
    private final HitReason hitReason;

    /**
     * 创建通过结果
     */
    public static EvaluationResult passed(HitReason hitReason) {
        return EvaluationResult.builder()
            .passed(true)
            .blocked(false)
            .hitReason(hitReason)
            .build();
    }

    /**
     * 创建失败结果
     */
    public static EvaluationResult failed(HitReason hitReason) {
        return EvaluationResult.builder()
            .passed(false)
            .blocked(false)
            .hitReason(hitReason)
            .build();
    }

    /**
     * 创建block结果
     */
    public static EvaluationResult blocked(String reason) {
        return EvaluationResult.builder()
            .passed(false)
            .blocked(true)
            .blockReason(reason)
            .build();
    }
}

