package com.qyl.v2trade.indicator.infrastructure.converter;

import com.qyl.v2trade.indicator.domain.event.BarClosedEvent;
import com.qyl.v2trade.indicator.infrastructure.resolver.TradingPairResolver;
import com.qyl.v2trade.market.aggregation.event.AggregatedKLine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * AggregatedKLine转BarClosedEvent转换器
 * 
 * <p>将聚合完成的K线转换为指标模块使用的BarClosedEvent
 *
 * @author qyl
 */
@Slf4j
@Component
public class AggregatedKLineToBarClosedEventConverter {
    
    @Autowired(required = false)
    private TradingPairResolver tradingPairResolver;
    
    /**
     * 转换
     * 
     * @param aggregatedKLine 聚合K线
     * @return BarClosedEvent
     */
    public BarClosedEvent convert(AggregatedKLine aggregatedKLine) {
        if (aggregatedKLine == null) {
            throw new IllegalArgumentException("aggregatedKLine不能为null");
        }
        
        // 获取tradingPairId
        Long tradingPairId = null;
        if (tradingPairResolver != null) {
            try {
                tradingPairId = tradingPairResolver.symbolToTradingPairId(aggregatedKLine.symbol());
                if (tradingPairId == null) {
                    log.warn("无法解析tradingPairId: symbol={}, 事件将继续发布但tradingPairId为null", aggregatedKLine.symbol());
                } else {
                    log.debug("成功解析tradingPairId: symbol={} -> tradingPairId={}", aggregatedKLine.symbol(), tradingPairId);
                }
            } catch (Exception e) {
                log.error("解析tradingPairId异常: symbol={}", aggregatedKLine.symbol(), e);
            }
        } else {
            log.warn("TradingPairResolver未注入，无法解析tradingPairId: symbol={}", aggregatedKLine.symbol());
        }
        
        // AggregatedKLine的timestamp是窗口起始时间（openTime）
        // 需要转换为bar_close_time（closeTime）
        // closeTime = openTime + timeframe_duration
        long timeframeDuration = getTimeframeDurationMillis(aggregatedKLine.period());
        long barCloseTimeMillis = aggregatedKLine.timestamp() + timeframeDuration;
        
        LocalDateTime barCloseTime = Instant.ofEpochMilli(barCloseTimeMillis)
                .atZone(ZoneId.of("UTC"))
                .toLocalDateTime();
        
        return BarClosedEvent.of(
                tradingPairId,
                aggregatedKLine.symbol(),
                aggregatedKLine.period(),
                barCloseTime,
                aggregatedKLine.open(),
                aggregatedKLine.high(),
                aggregatedKLine.low(),
                aggregatedKLine.close(),
                aggregatedKLine.volume(),
                aggregatedKLine.sourceKlineCount()
        );
    }
    
    /**
     * 获取周期对应的毫秒数
     */
    private long getTimeframeDurationMillis(String period) {
        if (period == null || period.isEmpty()) {
            return 60 * 1000L; // 默认1分钟
        }
        
        try {
            String numberStr = period.replaceAll("[^0-9]", "");
            String unit = period.replaceAll("[0-9]", "").toLowerCase();
            
            if (numberStr.isEmpty()) {
                return 60 * 1000L;
            }
            
            long number = Long.parseLong(numberStr);
            
            switch (unit) {
                case "m":
                    return number * 60 * 1000L;
                case "h":
                    return number * 60 * 60 * 1000L;
                case "d":
                    return number * 24 * 60 * 60 * 1000L;
                case "w":
                    return number * 7 * 24 * 60 * 60 * 1000L;
                case "M":
                    return number * 30L * 24 * 60 * 60 * 1000L;
                default:
                    log.warn("未知的周期单位: {}, 使用默认值 1 分钟", unit);
                    return 60 * 1000L;
            }
        } catch (Exception e) {
            log.warn("解析周期失败: period={}, 使用默认值 1 分钟", period, e);
            return 60 * 1000L;
        }
    }
}

