package com.qyl.v2trade.business.strategy.runtime.trigger;

import com.qyl.v2trade.business.signal.model.entity.Signal;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 策略触发器统一事件模型
 * 
 * <p>这是 N2 的"统一输入协议"。后续 N3/N5/N6 都复用它，避免返工。
 *
 * @author qyl
 */
public class StrategyTrigger {
    
    /**
     * 触发类型
     */
    private TriggerType triggerType;
    
    /**
     * 事件键（幂等键，必须全局可复现）
     */
    private String eventKey;
    
    /**
     * 统一 UTC 语义时间
     */
    private Instant asOfTimeUtc;
    
    /**
     * 交易对ID（BAR_CLOSE / PRICE 必填）
     */
    private Long tradingPairId;
    
    /**
     * 策略交易对符号（用于打印/排障，可选）
     */
    private String strategySymbol;
    
    /**
     * 时间周期（BAR_CLOSE 必填，如 "5m", "1h"）
     */
    private String timeframe;
    
    /**
     * 信号配置ID（SIGNAL 必填）
     */
    private Long signalConfigId;
    
    /**
     * 信号ID（SIGNAL 强烈建议提供，用于幂等与追踪）
     */
    private String signalId;
    
    /**
     * 价格（PRICE 必填；SIGNAL 可带）
     */
    private BigDecimal price;
    
    /**
     * K线开盘价（BAR_CLOSE 必填）
     */
    private BigDecimal barOpen;
    
    /**
     * K线最高价（BAR_CLOSE 必填）
     */
    private BigDecimal barHigh;
    
    /**
     * K线最低价（BAR_CLOSE 必填）
     */
    private BigDecimal barLow;
    
    /**
     * K线收盘价（BAR_CLOSE 必填）
     */
    private BigDecimal barClose;
    
    /**
     * K线成交量（BAR_CLOSE 可选）
     */
    private BigDecimal barVolume;
    
    /**
     * 信号详细信息（SIGNAL 可选）
     */
    private Signal signalInfo;
    
    // Getters and Setters
    
    public TriggerType getTriggerType() {
        return triggerType;
    }
    
    public void setTriggerType(TriggerType triggerType) {
        this.triggerType = triggerType;
    }
    
    public String getEventKey() {
        return eventKey;
    }
    
    public void setEventKey(String eventKey) {
        this.eventKey = eventKey;
    }
    
    public Instant getAsOfTimeUtc() {
        return asOfTimeUtc;
    }
    
    public void setAsOfTimeUtc(Instant asOfTimeUtc) {
        this.asOfTimeUtc = asOfTimeUtc;
    }
    
    public Long getTradingPairId() {
        return tradingPairId;
    }
    
    public void setTradingPairId(Long tradingPairId) {
        this.tradingPairId = tradingPairId;
    }
    
    public String getStrategySymbol() {
        return strategySymbol;
    }
    
    public void setStrategySymbol(String strategySymbol) {
        this.strategySymbol = strategySymbol;
    }
    
    public String getTimeframe() {
        return timeframe;
    }
    
    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }
    
    public Long getSignalConfigId() {
        return signalConfigId;
    }
    
    public void setSignalConfigId(Long signalConfigId) {
        this.signalConfigId = signalConfigId;
    }
    
    public String getSignalId() {
        return signalId;
    }
    
    public void setSignalId(String signalId) {
        this.signalId = signalId;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public BigDecimal getBarOpen() {
        return barOpen;
    }
    
    public void setBarOpen(BigDecimal barOpen) {
        this.barOpen = barOpen;
    }
    
    public BigDecimal getBarHigh() {
        return barHigh;
    }
    
    public void setBarHigh(BigDecimal barHigh) {
        this.barHigh = barHigh;
    }
    
    public BigDecimal getBarLow() {
        return barLow;
    }
    
    public void setBarLow(BigDecimal barLow) {
        this.barLow = barLow;
    }
    
    public BigDecimal getBarClose() {
        return barClose;
    }
    
    public void setBarClose(BigDecimal barClose) {
        this.barClose = barClose;
    }
    
    public BigDecimal getBarVolume() {
        return barVolume;
    }
    
    public void setBarVolume(BigDecimal barVolume) {
        this.barVolume = barVolume;
    }
    
    public Signal getSignalInfo() {
        return signalInfo;
    }
    
    public void setSignalInfo(Signal signalInfo) {
        this.signalInfo = signalInfo;
    }
}

