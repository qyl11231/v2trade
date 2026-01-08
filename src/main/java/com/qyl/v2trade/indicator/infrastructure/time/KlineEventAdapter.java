package com.qyl.v2trade.indicator.infrastructure.time;

import com.qyl.v2trade.indicator.domain.model.NormalizedBar;
import com.qyl.v2trade.indicator.infrastructure.resolver.TradingPairResolver;
import com.qyl.v2trade.market.model.event.KlineEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * KlineEvent适配器
 * 
 * <p>将KlineEvent转换为NormalizedBar
 * 
 * <p>转换规则：
 * - KlineEvent.closeTime 已经是 bar_close_time，直接使用
 *
 * @author qyl
 */
@Slf4j
@Component
public class KlineEventAdapter implements TimeAlignmentAdapter {
    
    @Autowired(required = false)
    private TradingPairResolver tradingPairResolver;
    
    @Override
    public boolean supports(Object rawBar) {
        return rawBar instanceof KlineEvent;
    }
    
    @Override
    public NormalizedBar normalize(Object rawBar) {
        if (!(rawBar instanceof KlineEvent)) {
            throw new IllegalArgumentException("不支持的Bar类型: " + rawBar.getClass());
        }
        
        KlineEvent event = (KlineEvent) rawBar;
        
        // KlineEvent.closeTime 已经是 bar_close_time
        ZonedDateTime barCloseTime = Instant.ofEpochMilli(event.closeTime())
                .atZone(ZoneId.of("UTC"));
        
        // 获取tradingPairId
        Long tradingPairId = null;
        if (tradingPairResolver != null) {
            try {
                tradingPairId = tradingPairResolver.symbolToTradingPairId(event.symbol());
            } catch (Exception e) {
                log.warn("无法解析tradingPairId: symbol={}", event.symbol(), e);
            }
        }
        
        return NormalizedBar.of(
                tradingPairId,
                event.symbol(),
                event.interval(),
                barCloseTime.toLocalDateTime(),
                event.open(),
                event.high(),
                event.low(),
                event.close(),
                event.volume()
        );
    }
}

