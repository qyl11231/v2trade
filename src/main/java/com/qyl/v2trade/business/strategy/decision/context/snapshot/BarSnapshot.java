package com.qyl.v2trade.business.strategy.decision.context.snapshot;

import lombok.Builder;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * K线快照（不可变）
 * 
 * <p>从 BarClosedEvent 获取的K线快照
 * 
 * <p>用于K线驱动策略的决策
 * 
 * <p>时间字段统一使用 {@link Instant} 类型，表示 UTC 时间点。
 */
@Getter
@Builder
public class BarSnapshot implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * K线周期（如：1m, 5m, 15m, 1h）
     */
    private final String timeframe;

    /**
     * K线收盘时间（bar_close_time，UTC）
     */
    private final Instant barCloseTime;

    /**
     * 开盘价
     */
    private final BigDecimal open;

    /**
     * 最高价
     */
    private final BigDecimal high;

    /**
     * 最低价
     */
    private final BigDecimal low;

    /**
     * 收盘价
     */
    private final BigDecimal close;

    /**
     * 成交量
     */
    private final BigDecimal volume;

    /**
     * 聚合使用的源K线数量
     */
    private final Integer sourceCount;

    /**
     * 事件产生时间（UTC）
     */
    private final Instant eventTime;
}

