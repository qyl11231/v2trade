package com.qyl.v2trade.business.strategy.runtime.snapshot;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 最新K线快照
 * 
 * <p>线程安全，只保存最新K线数据
 *
 * @author qyl
 */
public class LatestBarSnapshot {
    
    private volatile String timeframe;        // 1m, 5m, 1h 等
    private volatile BigDecimal open;
    private volatile BigDecimal high;
    private volatile BigDecimal low;
    private volatile BigDecimal close;
    private volatile BigDecimal volume;
    private volatile Instant barCloseTimeUtc; // K线闭合时间
    
    /**
     * 线程安全的更新方法
     * 
     * <p>只更新相同 timeframe 或更新的数据
     * 
     * @param tf 时间周期
     * @param open 开盘价
     * @param high 最高价
     * @param low 最低价
     * @param close 收盘价
     * @param volume 成交量
     * @param closeTime K线闭合时间
     */
    public synchronized void update(String tf, BigDecimal open, BigDecimal high, 
                                   BigDecimal low, BigDecimal close, BigDecimal volume, 
                                   Instant closeTime) {
        if (tf == null || closeTime == null) {
            return;
        }
        
        // 只更新相同 timeframe 或更新的数据
        if (this.timeframe == null || this.timeframe.equals(tf)) {
            // 只更新时间不早于当前快照的数据
            if (barCloseTimeUtc == null || !closeTime.isBefore(barCloseTimeUtc)) {
                this.timeframe = tf;
                this.open = open;
                this.high = high;
                this.low = low;
                this.close = close;
                this.volume = volume;
                this.barCloseTimeUtc = closeTime;
            }
        }
    }
    
    // Getters
    
    public String getTimeframe() {
        return timeframe;
    }
    
    public BigDecimal getOpen() {
        return open;
    }
    
    public BigDecimal getHigh() {
        return high;
    }
    
    public BigDecimal getLow() {
        return low;
    }
    
    public BigDecimal getClose() {
        return close;
    }
    
    public BigDecimal getVolume() {
        return volume;
    }
    
    public Instant getBarCloseTimeUtc() {
        return barCloseTimeUtc;
    }
    
    /**
     * 判断快照是否为空
     * 
     * @return true 表示未初始化
     */
    public boolean isEmpty() {
        return timeframe == null || barCloseTimeUtc == null;
    }
}

