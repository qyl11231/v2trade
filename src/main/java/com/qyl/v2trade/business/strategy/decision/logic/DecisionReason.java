package com.qyl.v2trade.business.strategy.decision.logic;

import lombok.Builder;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 决策原因（结构化信息）
 * 
 * <p>职责：
 * <ul>
 *   <li>存储决策的详细原因</li>
 *   <li>用于构建decision_reason JSON</li>
 *   <li>便于回放和审计</li>
 * </ul>
 * 
 * <p>注意：这是一个中间对象，最终会被序列化为JSON字符串
 */
@Getter
@Builder
public class DecisionReason implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 触发源类型：SIGNAL / INDICATOR / BAR / PRICE
     */
    private final String triggerType;

    /**
     * 触发源标识（signalIntentId / indicatorCode / barCloseTime / triggerType）
     */
    private final String triggerSource;

    /**
     * 触发时间戳
     */
    private final String triggerTimestamp;

    /**
     * 决策依据（Map格式，包含具体的判断条件）
     * 
     * <p>示例：
     * <ul>
     *   <li>信号驱动：{"signalDirection": "BUY", "currentPosition": "FLAT"}</li>
     *   <li>指标驱动：{"indicatorCode": "RSI_14", "value": 30.5, "condition": "RSI < 30"}</li>
     * </ul>
     */
    private final Map<String, Object> decisionBasis;

    /**
     * 状态变化说明
     * 
     * <p>示例："从FLAT状态开仓LONG"
     */
    private final String stateChange;

    /**
     * 其他信息（扩展字段）
     */
    private final Map<String, Object> extra;
}

