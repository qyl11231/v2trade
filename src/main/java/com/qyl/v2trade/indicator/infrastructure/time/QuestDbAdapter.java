package com.qyl.v2trade.indicator.infrastructure.time;

import com.qyl.v2trade.indicator.domain.model.NormalizedBar;
import com.qyl.v2trade.indicator.infrastructure.resolver.TradingPairResolver;
import com.qyl.v2trade.market.model.NormalizedKline;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * QuestDB适配器
 * 
 * <p>将QuestDB读取的NormalizedKline转换为NormalizedBar
 * 
 * <p>转换规则：
 * - 根据QuestDbTsSemanticsProbe检测的结果转换
 * - TS_IS_OPEN_TIME: bar_close_time = ts + timeframe_duration
 * - TS_IS_CLOSE_TIME: bar_close_time = ts
 *
 * @author qyl
 */
@Slf4j
@Component
public class QuestDbAdapter implements TimeAlignmentAdapter {
    
    @Autowired(required = false)
    private TradingPairResolver tradingPairResolver;
    
    @Override
    public boolean supports(Object rawBar) {
        return rawBar instanceof NormalizedKline;
    }
    
    @Override
    public NormalizedBar normalize(Object rawBar) {
        if (!(rawBar instanceof NormalizedKline)) {
            throw new IllegalArgumentException("不支持的Bar类型: " + rawBar.getClass());
        }
        
        NormalizedKline kline = (NormalizedKline) rawBar;
        
        // 获取检测到的语义
        QuestDbTsSemantics semantics = QuestDbTsSemanticsProbe.getDetectedSemantics();
        
        if (semantics == QuestDbTsSemantics.UNKNOWN) {
            throw new IllegalStateException(
                "QuestDB ts字段语义未检测，无法转换。请确保QuestDbTsSemanticsProbe已运行。"
            );
        }
        
        // 计算bar_close_time
        long barCloseTimeMillis;
        
        if (semantics == QuestDbTsSemantics.TS_IS_OPEN_TIME) {
            // ts是开盘时间，需要加上周期时长
            long timeframeDuration = getTimeframeDurationMillis(kline.getInterval());
            barCloseTimeMillis = kline.getTimestamp() + timeframeDuration;
        } else {
            // TS_IS_CLOSE_TIME: ts就是收盘时间
            barCloseTimeMillis = kline.getTimestamp();
        }
        
        LocalDateTime barCloseTime = Instant.ofEpochMilli(barCloseTimeMillis)
                .atZone(ZoneId.of("UTC"))
                .toLocalDateTime();
        
        // 获取tradingPairId
        Long tradingPairId = null;
        if (tradingPairResolver != null) {
            try {
                tradingPairId = tradingPairResolver.symbolToTradingPairId(kline.getSymbol());
            } catch (Exception e) {
                log.warn("无法解析tradingPairId: symbol={}", kline.getSymbol(), e);
            }
        }
        
        return NormalizedBar.of(
                tradingPairId,
                kline.getSymbol(),
                kline.getInterval(),
                barCloseTime,
                BigDecimal.valueOf(kline.getOpen()),
                BigDecimal.valueOf(kline.getHigh()),
                BigDecimal.valueOf(kline.getLow()),
                BigDecimal.valueOf(kline.getClose()),
                BigDecimal.valueOf(kline.getVolume())
        );
    }
    
    /**
     * 获取周期对应的毫秒数
     */
    private long getTimeframeDurationMillis(String interval) {
        if (interval == null || interval.isEmpty()) {
            return 60 * 1000L; // 默认1分钟
        }
        
        try {
            // 提取数字和单位
            String numberStr = interval.replaceAll("[^0-9]", "");
            String unit = interval.replaceAll("[0-9]", "").toLowerCase();
            
            if (numberStr.isEmpty()) {
                return 60 * 1000L;
            }
            
            long number = Long.parseLong(numberStr);
            
            switch (unit) {
                case "m":
                    return number * 60 * 1000L; // 分钟转毫秒
                case "h":
                    return number * 60 * 60 * 1000L; // 小时转毫秒
                case "d":
                    return number * 24 * 60 * 60 * 1000L; // 天转毫秒
                case "w":
                    return number * 7 * 24 * 60 * 60 * 1000L; // 周转毫秒
                case "M":
                    return number * 30L * 24 * 60 * 60 * 1000L; // 月转毫秒（简化处理）
                default:
                    log.warn("未知的周期单位: {}, 使用默认值 1 分钟", unit);
                    return 60 * 1000L;
            }
        } catch (Exception e) {
            log.warn("解析周期失败: interval={}, 使用默认值 1 分钟", interval, e);
            return 60 * 1000L;
        }
    }
}

