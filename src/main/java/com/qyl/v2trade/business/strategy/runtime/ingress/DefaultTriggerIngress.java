package com.qyl.v2trade.business.strategy.runtime.ingress;

import com.qyl.v2trade.business.strategy.runtime.dispatcher.TriggerDispatcher;
import com.qyl.v2trade.business.strategy.runtime.trigger.StrategyTrigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 默认事件接入实现
 * 
 * <p>统一的事件入口，接收事件并分发到异步队列
 *
 * @author qyl
 */
@Component
public class DefaultTriggerIngress implements TriggerIngress {
    
    @Autowired
    private TriggerDispatcher dispatcher;
    
    @Override
    public void accept(StrategyTrigger trigger) {
        // 轻量校验
        if (trigger == null || trigger.getEventKey() == null) {
            return;
        }
        
        // 直接分发到异步队列
        dispatcher.dispatch(trigger);
    }
}

