package com.qyl.v2trade.business.strategy.runtime.ingress;

import com.qyl.v2trade.business.strategy.runtime.trigger.StrategyTrigger;

/**
 * 事件接入接口
 * 
 * <p>统一的事件入口，接收事件（轻量转换，不查库、不路由、不重逻辑）
 *
 * @author qyl
 */
public interface TriggerIngress {
    
    /**
     * 接收事件
     * 
     * @param trigger 策略触发器事件
     */
    void accept(StrategyTrigger trigger);
}

