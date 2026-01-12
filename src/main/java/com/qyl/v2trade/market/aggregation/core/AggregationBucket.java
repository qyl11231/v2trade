package com.qyl.v2trade.market.aggregation.core;

import com.qyl.v2trade.market.aggregation.config.SupportedPeriod;
import com.qyl.v2trade.market.aggregation.event.AggregatedKLine;
import com.qyl.v2trade.market.model.event.KlineEvent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 聚合桶（内存状态）
 * 
 * <p>职责：维护一个时间窗口内的聚合状态
 * 
 * <p>生命周期：窗口开始 -> 持续更新 -> 窗口结束 -> 生成聚合结果 -> 清理
 * 
 * <p>线程安全：使用synchronized保护状态更新操作
 *
 * @author qyl
 */
public class AggregationBucket {
    
    /**
     * 交易对符号
     */
    private final String symbol;
    
    /**
     * 聚合周期
     */
    private final String period;
    
    /**
     * 窗口起始时间戳（毫秒）
     */
    private final long windowStart;
    
    /**
     * 窗口结束时间戳（毫秒）
     */
    private final long windowEnd;
    
    /**
     * 开盘价（第一根K线的开盘价，保持不变）
     */
    private BigDecimal open;
    
    /**
     * 最高价（持续更新）
     */
    private BigDecimal high;
    
    /**
     * 最低价（持续更新）
     */
    private BigDecimal low;
    
    /**
     * 收盘价（最后一根K线的收盘价，持续更新）
     */
    private BigDecimal close;
    
    /**
     * 累计成交量
     */
    private BigDecimal volume;
    
    /**
     * 已聚合的1m K线数量
     */
    private int klineCount;
    
    /**
     * 窗口内维护的1m K线事件集合
     */
    private final List<KlineEvent> klines;
    
    /**
     * 窗口是否已关闭
     */
    private volatile boolean isComplete;
    
    /**
     * 构造函数
     * 
     * @param symbol 交易对符号
     * @param period 周期字符串（如：5m, 15m）
     * @param windowStart 窗口起始时间戳（毫秒）
     * @param windowEnd 窗口结束时间戳（毫秒）
     */
    public AggregationBucket(String symbol, String period, long windowStart, long windowEnd) {
        this.symbol = symbol;
        this.period = period;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.open = null;
        this.high = null;
        this.low = null;
        this.close = null;
        this.volume = BigDecimal.ZERO;
        this.klineCount = 0;
        this.klines = new ArrayList<>();
        this.isComplete = false;
    }
    
    /**
     * 更新Bucket状态
     * 
     * <p>聚合规则：
     * <ul>
     *   <li>open: 第一根K线的open（保持不变）</li>
     *   <li>high: max(bucket.high, kline.high)</li>
     *   <li>low: min(bucket.low, kline.low)</li>
     *   <li>close: kline.close（持续更新）</li>
     *   <li>volume: bucket.volume + kline.volume</li>
     *   <li>klineCount: bucket.klineCount + 1</li>
     * </ul>
     * 
     * @param event 1m K线事件
     * @return 是否触发了窗口关闭（kline.openTime >= windowEnd）
     */
    public synchronized boolean update(KlineEvent event) {
        if (isComplete) {
            throw new IllegalStateException("Bucket已经关闭，不能继续更新: symbol=" + symbol + ", period=" + period + ", windowStart=" + windowStart);
        }
        
        // 第一根K线：设置open
        if (open == null) {
            open = event.open();
            high = event.high();
            low = event.low();
            close = event.close();
        } else {
            // 更新high/low/close
            high = high.max(event.high());
            low = low.min(event.low());
            close = event.close();
        }
        
        // 累计成交量
        volume = volume.add(event.volume());
        
        // 增加K线计数
        klineCount++;
        
        // 将K线事件添加到集合中
        klines.add(event);
        
        // 判断窗口是否结束
        // 规则：如果 kline.closeTime >= windowEnd，窗口结束
        // 重构：event.closeTime() 返回 Instant，需要转换为 long
        boolean windowComplete = event.closeTime().toEpochMilli() >= windowEnd;
        if (windowComplete) {
            isComplete = true;
        }
        
        return windowComplete;
    }
    
    /**
     * 判断窗口是否结束
     * 
     * @param klineTimestamp 1m K线时间戳（毫秒）
     * @return 如果 klineTimestamp >= windowEnd，返回true
     */
    public boolean isWindowComplete(long klineTimestamp) {
        return klineTimestamp >= windowEnd;
    }
    
    /**
     * 判断是否过期（用于清理）
     * 
     * <p>规则：窗口结束超过1小时后，认为过期
     * 
     * @param currentTime 当前时间戳（毫秒）
     * @return 如果过期返回true
     */
    public boolean isExpired(long currentTime) {
        if (!isComplete) {
            return false; // 未完成的窗口不过期
        }
        // 窗口结束超过1小时后过期
        long expirationTime = windowEnd + (60 * 60 * 1000L); // 1小时
        return currentTime > expirationTime;
    }
    
    /**
     * 生成聚合K线事件
     * 
     * <p>【重要】时间戳必须对齐到窗口起始时间（使用PeriodCalculator.alignTimestamp()）
     * 
     * @return AggregatedKLine事件，如果Bucket为空返回null
     */
    public synchronized AggregatedKLine toAggregatedKLine() {
        if (open == null || klineCount == 0) {
            return null; // Bucket为空，不生成聚合结果
        }
        
        // 时间戳对齐到窗口起始时间
        SupportedPeriod periodEnum = SupportedPeriod.fromPeriod(period);
        if (periodEnum == null) {
            throw new IllegalArgumentException("不支持的周期: " + period);
        }
        long alignedTimestamp = PeriodCalculator.alignTimestamp(windowStart, periodEnum);
        
        return AggregatedKLine.of(
                symbol,
                period,
                alignedTimestamp,
                open,
                high,
                low,
                close,
                volume,
                klineCount
        );
    }
    
    // Getter方法
    
    public String getSymbol() {
        return symbol;
    }
    
    public String getPeriod() {
        return period;
    }
    
    public long getWindowStart() {
        return windowStart;
    }
    
    public long getWindowEnd() {
        return windowEnd;
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
    
    public int getKlineCount() {
        return klineCount;
    }
    
    public boolean isComplete() {
        return isComplete;
    }
    
    /**
     * 获取窗口内维护的1m K线事件集合
     * 
     * @return K线事件集合的副本（避免外部修改）
     */
    public synchronized List<KlineEvent> getKlines() {
        return new ArrayList<>(klines);
    }
}

