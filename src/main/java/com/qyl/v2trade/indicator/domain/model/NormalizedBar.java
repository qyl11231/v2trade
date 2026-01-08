package com.qyl.v2trade.indicator.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 标准化的Bar（指标模块内部使用）
 * 
 * <p>所有进入指标模块的Bar必须经过TimeAlignmentAdapter归一化
 * barTime必须是bar_close_time语义
 *
 * @author qyl
 */
public record NormalizedBar(
    /**
     * 交易对ID
     */
    Long tradingPairId,
    
    /**
     * 交易对符号
     */
    String symbol,
    
    /**
     * K线周期
     */
    String timeframe,
    
    /**
     * Bar收盘时间（bar_close_time，UTC）
     * 
     * <p>【重要】这是指标模块统一使用的时间语义
     */
    LocalDateTime barTime,
    
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
    BigDecimal volume
) {
    /**
     * 创建NormalizedBar
     */
    public static NormalizedBar of(
            Long tradingPairId,
            String symbol,
            String timeframe,
            LocalDateTime barTime,
            BigDecimal open,
            BigDecimal high,
            BigDecimal low,
            BigDecimal close,
            BigDecimal volume) {
        return new NormalizedBar(
                tradingPairId,
                symbol,
                timeframe,
                barTime,
                open,
                high,
                low,
                close,
                volume
        );
    }
}

