package com.qyl.v2trade.business.strategy.binder.model;

import com.qyl.v2trade.common.constants.ConsumeModeEnum;
import lombok.Builder;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 信号视图（只读观察关系）
 * 
 * <p>职责：
 * <ul>
 *   <li>建立策略实例与信号配置的观察关系</li>
 *   <li>标记为只读，不消费信号，不修改信号状态</li>
 *   <li>阶段1只建立关系，不拉取信号数据</li>
 * </ul>
 * 
 * <p>阶段1约束：
 * <ul>
 *   <li>只读视图，不消费信号</li>
 *   <li>不调用 SignalService.consume()</li>
 *   <li>不修改 signal_intent 状态</li>
 * </ul>
 */
@Getter
@Builder
public class SignalView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 信号配置ID
     */
    private final Long signalConfigId;

    /**
     * 消费模式（LATEST_ONLY）
     */
    private final String consumeMode;

    /**
     * 是否只读（阶段1必须为true）
     */
    @Builder.Default
    private final boolean readOnly = true;

    /**
     * 获取消费模式枚举
     * 
     * @return 消费模式枚举
     */
    public ConsumeModeEnum getConsumeModeEnum() {
        if (consumeMode == null) {
            return null;
        }
        return ConsumeModeEnum.fromCode(consumeMode);
    }
}

