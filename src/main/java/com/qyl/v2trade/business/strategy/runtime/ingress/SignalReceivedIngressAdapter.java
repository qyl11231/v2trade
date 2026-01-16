package com.qyl.v2trade.business.strategy.runtime.ingress;

import com.qyl.v2trade.business.signal.model.entity.Signal;
import com.qyl.v2trade.business.strategy.runtime.trigger.EventKeyBuilder;
import com.qyl.v2trade.business.strategy.runtime.trigger.StrategyTrigger;
import com.qyl.v2trade.business.strategy.runtime.trigger.TriggerType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;

/**
 * 信号接收事件接入适配器
 * 
 * <p>从信号模块接收信号事件
 *
 * @author qyl
 */
@Slf4j
@Component
public class SignalReceivedIngressAdapter {
    
    @Autowired
    private TriggerIngress triggerIngress;
    
    /**
     * 从信号模块接收信号事件
     * 
     * 注意：这个方法由 SignalService.ingestSignal 在信号入库成功后调用
     * 
     * @param signal 已入库的信号实体
     */
    public void onSignalReceived(Signal signal) {
        try {
            // 1. 获取必要字段
            Long signalConfigId = signal.getSignalConfigId();
            if (signalConfigId == null) {
                log.warn("信号缺少 signalConfigId，跳过事件: signalId={}", signal.getId());
                return;
            }
            
            // 2. 生成 signalId（优先使用 signal.id，如果没有则使用其他唯一标识）
            String signalId = String.valueOf(signal.getId());
            // 如果有其他唯一标识，可以使用
            
            // 3. 生成 eventKey
            String eventKey = EventKeyBuilder.buildSignalKey(signalConfigId, signalId);
            
            // 4. 获取接收时间（UTC）
            Instant receivedTimeUtc = signal.getReceivedAt() != null 
                ? signal.getReceivedAt().atZone(ZoneId.systemDefault()).toInstant()
                : Instant.now();
            
            // 5. 构建 StrategyTrigger
            StrategyTrigger trigger = new StrategyTrigger();
            trigger.setTriggerType(TriggerType.SIGNAL);
            trigger.setEventKey(eventKey);
            trigger.setAsOfTimeUtc(receivedTimeUtc);
            trigger.setSignalConfigId(signalConfigId);
            trigger.setSignalId(signalId);
            trigger.setSignalInfo(signal);
            
            // 如果有价格信息
            if (signal.getPrice() != null) {
                trigger.setPrice(signal.getPrice());
            }
            
            // 6. 发送到事件系统
            triggerIngress.accept(trigger);
            
        } catch (Exception e) {
            log.error("处理信号事件失败: signalId={}, signalConfigId={}", 
                signal.getId(), signal.getSignalConfigId(), e);
        }
    }
    
    /**
     * 测试方法：手动触发信号事件
     * 
     * @param signalConfigId 信号配置ID
     * @param signalId 信号ID（必须唯一）
     * @param price 信号价格（可选）
     */
    public void testSignal(Long signalConfigId, String signalId, java.math.BigDecimal price) {
        try {
            // 生成 eventKey
            String eventKey = EventKeyBuilder.buildSignalKey(signalConfigId, signalId);
            
            // 获取接收时间（UTC）
            Instant receivedTimeUtc = Instant.now();
            
            // 构建 StrategyTrigger
            StrategyTrigger trigger = new StrategyTrigger();
            trigger.setTriggerType(TriggerType.SIGNAL);
            trigger.setEventKey(eventKey);
            trigger.setAsOfTimeUtc(receivedTimeUtc);
            trigger.setSignalConfigId(signalConfigId);
            trigger.setSignalId(signalId);
            
            // 如果有价格信息
            if (price != null) {
                trigger.setPrice(price);
            }
            
            // 发送到事件系统
            triggerIngress.accept(trigger);
            
            log.info("测试信号事件已发送: signalConfigId={}, signalId={}", signalConfigId, signalId);
        } catch (Exception e) {
            log.error("测试信号事件失败: signalConfigId={}, signalId={}", signalConfigId, signalId, e);
            throw new RuntimeException("测试信号事件失败", e);
        }
    }
}

