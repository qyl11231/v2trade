package com.qyl.v2trade.business.strategy.runtime.snapshot;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 最新价格快照
 * 
 * <p>线程安全，只保存最新价格数据
 *
 * @author qyl
 */
public class LatestPriceSnapshot {
    
    private volatile BigDecimal price;
    private volatile Instant asOfTimeUtc;
    private volatile String source;  // 行情来源（如 OKX）
    
    /**
     * 线程安全的更新方法
     * 
     * <p>只更新时间不早于当前快照的数据
     * 
     * @param newPrice 新价格
     * @param newTime 新时间（UTC）
     * @param source 行情来源
     */
    public synchronized void update(BigDecimal newPrice, Instant newTime, String source) {
        if (newPrice == null || newTime == null) {
            return;
        }
        
        // 只更新时间不早于当前快照的数据
        if (asOfTimeUtc == null || !newTime.isBefore(asOfTimeUtc)) {
            this.price = newPrice;
            this.asOfTimeUtc = newTime;
            this.source = source;
        }
    }
    
    // Getters
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public Instant getAsOfTimeUtc() {
        return asOfTimeUtc;
    }
    
    public String getSource() {
        return source;
    }
    
    /**
     * 判断快照是否为空
     * 
     * @return true 表示未初始化
     */
    public boolean isEmpty() {
        return price == null || asOfTimeUtc == null;
    }
}

