package com.qyl.v2trade.business.strategy.runtime.logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qyl.v2trade.business.strategy.runtime.trigger.StrategyTrigger;
import com.qyl.v2trade.business.strategy.runtime.trigger.TriggerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一日志打印器
 * 
 * <p>打印事件路由日志（JSON 单行格式），可 grep、可审计、可压测
 *
 * @author qyl
 */
@Component
public class TriggerLogger {
    
    private static final Logger logger = LoggerFactory.getLogger("STRATEGY_TRIGGER");
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 打印事件路由日志（JSON 单行格式）
     * 
     * 【重要】此方法不查库，userId 等信息由 EventRouter 在路由时提供
     * 避免一个事件路由到 N 个实例时产生 N 次 DB 查询，导致 DB 热点
     * 
     * @param trigger 事件
     * @param instanceId 实例ID
     * @param userId 用户ID（从路由缓存中获取，不查库）
     */
    public void log(StrategyTrigger trigger, Long instanceId, Long userId) {
        try {

            if(trigger.getTriggerType().equals(TriggerType.PRICE)){
                return;
            }
            // 构建日志数据
            Map<String, Object> logData = new HashMap<>();
            logData.put("type", trigger.getTriggerType().name());
            logData.put("eventKey", trigger.getEventKey());
            logData.put("asOf", trigger.getAsOfTimeUtc().toString());
            logData.put("pairId", trigger.getTradingPairId());
            logData.put("instanceId", instanceId);
            logData.put("userId", userId);
            
            // 可选字段
            if (trigger.getStrategySymbol() != null) {
                logData.put("strategySymbol", trigger.getStrategySymbol());
            }
            if (trigger.getTimeframe() != null) {
                logData.put("tf", trigger.getTimeframe());
            }
            if (trigger.getSignalConfigId() != null) {
                logData.put("signalConfigId", trigger.getSignalConfigId());
            }
            if (trigger.getSignalId() != null) {
                logData.put("signalId", trigger.getSignalId());
            }
            if (trigger.getPrice() != null) {
                logData.put("price", trigger.getPrice().toString());
            }
            
            // 打印 JSON 单行日志
            String json = objectMapper.writeValueAsString(logData);
            logger.info("route_print {}", json);
            
        } catch (JsonProcessingException e) {
            logger.error("序列化日志数据失败: instanceId={}, eventKey={}", 
                instanceId, trigger.getEventKey(), e);
        } catch (Exception e) {
            logger.error("打印日志失败: instanceId={}, eventKey={}", 
                instanceId, trigger.getEventKey(), e);
        }
    }
}

