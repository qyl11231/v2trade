package com.qyl.v2trade.market.aggregation.listener;

import com.qyl.v2trade.market.aggregation.core.KlineAggregator;
import com.qyl.v2trade.market.model.event.KlineEvent;
import com.qyl.v2trade.market.subscription.collector.eventbus.MarketEventBus;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * K线事件消费者
 * 
 * <p>订阅MarketEventBus的KlineEvent，过滤1m K线事件，传递给KlineAggregator处理
 *
 * @author qyl
 */
@Slf4j
@Component
public class KlineEventConsumer {
    
    @Autowired
    private MarketEventBus marketEventBus;
    
    @Autowired
    private KlineAggregator klineAggregator;
    
    /**
     * 事件消费者（保存引用以便取消订阅）
     */
    private Consumer<KlineEvent> eventConsumer;
    
    @PostConstruct
    public void subscribe() {
        eventConsumer = this::handleKlineEvent;
        marketEventBus.subscribe(eventConsumer);
        log.info("KlineEventConsumer已订阅MarketEventBus");
    }
    
    @PreDestroy
    public void unsubscribe() {
        if (eventConsumer != null) {
            marketEventBus.unsubscribe(eventConsumer);
            log.info("KlineEventConsumer已取消订阅MarketEventBus");
        }
    }
    
    /**
     * 处理K线事件
     * 
     * @param event K线事件
     */
    private void handleKlineEvent(KlineEvent event) {
        try {
            // 只处理1m K线
            if (!"1m".equals(event.interval())) {
                log.debug("跳过非1m K线事件: interval={}, symbol={}, openTime={}", 
                        event.interval(), event.symbol(), event.openTime());
                return;
            }
            
            // 调用聚合器处理
            klineAggregator.onKlineEvent(event);
            
        } catch (Exception e) {
            log.error("处理K线事件异常: symbol={}, interval={}, openTime={}", 
                    event.symbol(), event.interval(), event.openTime(), e);
        }
    }
}

