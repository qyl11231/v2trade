package com.qyl.v2trade.business.strategy.decision.guard;

import lombok.Builder;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 门禁校验结果
 * 
 * <p>职责：
 * <ul>
 *   <li>表示门禁校验的结果（允许/拒绝）</li>
 *   <li>如果拒绝，包含拒绝原因</li>
 * </ul>
 */
@Getter
@Builder
public class GuardResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 是否允许通过
     */
    private final boolean allowed;

    /**
     * 拒绝的门禁名称（如果allowed=false）
     */
    private final String rejectedGate;

    /**
     * 拒绝原因（如果allowed=false）
     */
    private final String reason;

    /**
     * 创建允许通过的校验结果
     * 
     * @return 允许通过的校验结果
     */
    public static GuardResult allowed() {
        return GuardResult.builder()
            .allowed(true)
            .rejectedGate(null)
            .reason(null)
            .build();
    }

    /**
     * 创建拒绝通过的校验结果
     * 
     * @param gateName 拒绝的门禁名称
     * @param reason 拒绝原因
     * @return 拒绝通过的校验结果
     */
    public static GuardResult rejected(String gateName, String reason) {
        return GuardResult.builder()
            .allowed(false)
            .rejectedGate(gateName)
            .reason(reason)
            .build();
    }
}

