package com.qyl.v2trade.business.strategy.decision.context.snapshot;

import lombok.Builder;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 价格快照（不可变）
 * 
 * <p>从内存价格快照获取的最新价格
 * 
 * <p>用于价格触发决策（止盈止损）
 * 
 * <p>注意：价格可能为空（如果价格服务未就绪）
 */
@Getter
@Builder
public class PriceSnapshot implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 当前价格
     */
    private final BigDecimal currentPrice;

    /**
     * 价格时间戳
     */
    private final LocalDateTime priceTime;

    /**
     * 价格来源（如：OKX, BINANCE）
     */
    private final String source;

    /**
     * 判断价格是否可用
     */
    public boolean isAvailable() {
        return currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0;
    }
}

