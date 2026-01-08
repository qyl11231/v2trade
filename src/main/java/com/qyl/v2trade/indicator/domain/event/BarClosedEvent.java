package com.qyl.v2trade.indicator.domain.event;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bar闭合事件
 * 
 * <p>表示一根K线已经完成，可以用于指标计算
 * 
 * <p>【重要】指标模块只订阅此事件，不订阅forming更新
 *
 * @author qyl
 */
public record BarClosedEvent(
    /**
     * 交易对ID（系统内部引用）
     */
    Long tradingPairId,
    
    /**
     * 交易对符号（如：BTC-USDT-SWAP）
     */
    String symbol,
    
    /**
     * K线周期（如：1m, 5m, 15m, 1h）
     */
    String timeframe,
    
    /**
     * K线收盘时间（bar_close_time，UTC）
     * 
     * <p>【重要】这是指标模块统一使用的时间语义
     */
    LocalDateTime barCloseTime,
    
    /**
     * 开盘价
     */
    BigDecimal open,
    
    /**
     * 最高价
     */
    BigDecimal high,
    
    /**
     * 最低价
     */
    BigDecimal low,
    
    /**
     * 收盘价
     */
    BigDecimal close,
    
    /**
     * 成交量
     */
    BigDecimal volume,
    
    /**
     * 聚合使用的源K线数量（1m周期为1，5m周期为5等）
     */
    int sourceCount,
    
    /**
     * 事件产生时间（本地时间）
     */
    LocalDateTime eventTime
) implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * 创建BarClosedEvent
     */
    public static BarClosedEvent of(
            Long tradingPairId,
            String symbol,
            String timeframe,
            LocalDateTime barCloseTime,
            BigDecimal open,
            BigDecimal high,
            BigDecimal low,
            BigDecimal close,
            BigDecimal volume,
            int sourceCount) {
        return new BarClosedEvent(
                tradingPairId,
                symbol,
                timeframe,
                barCloseTime,
                open,
                high,
                low,
                close,
                volume,
                sourceCount,
                LocalDateTime.now()
        );
    }
}

