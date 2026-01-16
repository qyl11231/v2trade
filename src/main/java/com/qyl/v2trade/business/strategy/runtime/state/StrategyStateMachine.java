package com.qyl.v2trade.business.strategy.runtime.state;

import com.qyl.v2trade.business.strategy.runtime.trigger.StrategyTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * 策略状态机
 * 
 * <p>N3 最小实现：只更新 lastEventTime，不推进 phase
 * 
 * <p>【硬约束】：N3 不主动推进 phase（IDLE → OPEN_PENDING 等，N6 才做）
 *
 * @author qyl
 */
public class StrategyStateMachine {
    
    private static final Logger log = LoggerFactory.getLogger(StrategyStateMachine.class);
    
    private StrategyState currentState;
    
    public StrategyStateMachine(StrategyState initialState) {
        this.currentState = initialState;
    }
    
    /**
     * 处理触发事件（N3 最小实现：只更新 lastEventTime，不推进 phase）
     * 
     * 【硬约束】：N3 不主动推进 phase（IDLE → OPEN_PENDING 等，N6 才做）
     * 
     * @param trigger 触发事件
     */
    public void onTrigger(StrategyTrigger trigger) {
        // N3 阶段只做最小维护：
        // 1. 更新最后事件时间
        currentState.setLastEventTimeUtc(trigger.getAsOfTimeUtc());
        
        // 2. N3 不做 phase 迁移（留给 N6/N7）
        // 如果 DB 恢复的状态是 OPENED，就保持 OPENED
        
        log.debug("状态机处理事件: phase={}, eventType={}, asOf={}", 
            currentState.getPhase(), trigger.getTriggerType(), trigger.getAsOfTimeUtc());
    }
    
    /**
     * 获取当前状态（只读）
     * 
     * @return 当前状态
     */
    public StrategyState getCurrentState() {
        return currentState;
    }
    
    /**
     * 设置状态（用于外部注入，如 N6/N7 阶段）
     * 
     * <p>注意：此方法 N3 阶段不使用，为后续扩展预留
     * 
     * @param phase 策略阶段
     * @param side 持仓方向
     * @param qty 持仓数量
     * @param avg 平均开仓价
     */
    public void setState(StrategyPhase phase, String side, BigDecimal qty, BigDecimal avg) {
        // TODO: N6/N7 阶段实现
        throw new UnsupportedOperationException("N3 阶段不支持状态迁移");
    }
}

