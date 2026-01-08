package com.qyl.v2trade.indicator.engine;

import java.util.Map;

/**
 * 指标计算请求
 *
 * @author qyl
 */
public record IndicatorComputeRequest(
    /**
     * 指标编码（如：RSI, MACD, SMA）
     */
    String indicatorCode,
    
    /**
     * 指标版本
     */
    String indicatorVersion,
    
    /**
     * 指标参数（如：{"period": 14}）
     */
    Map<String, Object> parameters,
    
    /**
     * 交易对ID
     */
    Long tradingPairId,
    
    /**
     * 周期（如：1m, 5m, 1h）
     */
    String timeframe,
    
    /**
     * 目标Bar时间（bar_close_time，UTC）
     * 
     * <p>计算该时间点的指标值
     */
    java.time.LocalDateTime targetBarTime
) {
}

