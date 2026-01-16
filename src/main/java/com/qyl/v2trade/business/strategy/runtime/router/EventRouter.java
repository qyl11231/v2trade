package com.qyl.v2trade.business.strategy.runtime.router;

import com.qyl.v2trade.business.strategy.runtime.trigger.StrategyTrigger;

import java.util.List;

/**
 * 事件路由接口
 * 
 * <p>将事件路由到对应的 strategy_instance_id 列表
 *
 * @author qyl
 */
public interface EventRouter {
    
    /**
     * 路由事件到对应的 strategy_instance_id 列表
     * 
     * @param trigger 事件
     * @return 受影响的实例ID列表（只返回 enabled 的实例）
     */
    List<Long> route(StrategyTrigger trigger);
    
    /**
     * 根据 instanceId 获取 userId（从缓存获取，不查库）
     * 用于 TriggerLogger，避免 per-instance 查库导致 DB 热点
     * 
     * @param instanceId 实例ID
     * @return 用户ID，如果缓存中不存在则返回 null
     */
    Long getUserIdByInstanceId(Long instanceId);
}

