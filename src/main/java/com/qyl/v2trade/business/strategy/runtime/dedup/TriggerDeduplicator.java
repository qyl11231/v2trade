package com.qyl.v2trade.business.strategy.runtime.dedup;

/**
 * 事件去重器接口
 * 
 * <p>用于判断事件是否应该处理（30秒内相同 eventKey 只处理一次）
 *
 * @author qyl
 */
public interface TriggerDeduplicator {
    
    /**
     * 判断是否应该处理该事件
     * 
     * @param eventKey 事件键
     * @return true 表示应该处理，false 表示重复事件应跳过
     */
    boolean shouldProcess(String eventKey);
}

