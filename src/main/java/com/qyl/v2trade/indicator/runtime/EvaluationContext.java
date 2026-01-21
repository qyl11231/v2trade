package com.qyl.v2trade.indicator.runtime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评估上下文（V2新增）
 * 
 * <p>包含评估所需的所有上下文信息
 * 
 * <p>【时间语义】asOfBarTime 是 bar_close_time UTC 语义（指标模块统一时间语义）
 *
 * @author qyl
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationContext {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 交易对ID
     */
    private Long tradingPairId;
    
    /**
     * 周期（如：5m、15m、30m、1h、4h）
     */
    private String timeframe;
    
    /**
     * 目标Bar收盘时间（bar_close_time，UTC）
     * 
     * <p>【重要】这是指标模块统一使用的时间语义
     * <p>计算该时间点的指标值
     */
    private LocalDateTime asOfBarTime;
}

