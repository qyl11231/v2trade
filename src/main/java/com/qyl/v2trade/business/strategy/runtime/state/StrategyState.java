package com.qyl.v2trade.business.strategy.runtime.state;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * 策略状态内存模型
 * 
 * <p>用于 Runtime 内存维护，不直接映射数据库
 * 
 * <p>包含变化检测逻辑，避免频繁写库
 *
 * @author qyl
 */
public class StrategyState {
    
    /**
     * 策略阶段
     */
    private StrategyPhase phase;
    
    /**
     * 持仓方向：LONG/SHORT/FLAT
     */
    private String positionSide;
    
    /**
     * 持仓数量
     */
    private BigDecimal positionQty;
    
    /**
     * 平均开仓价
     */
    private BigDecimal avgEntryPrice;
    
    /**
     * 最后处理事件时间（UTC，内存维护，不持久化）
     */
    private Instant lastEventTimeUtc;
    
    /**
     * 已持久化的状态哈希（用于变化检测）
     */
    private String persistedStateHash;
    
    // Getters and Setters
    
    public StrategyPhase getPhase() {
        return phase;
    }
    
    public void setPhase(StrategyPhase phase) {
        this.phase = phase;
    }
    
    public String getPositionSide() {
        return positionSide;
    }
    
    public void setPositionSide(String positionSide) {
        this.positionSide = positionSide;
    }
    
    public BigDecimal getPositionQty() {
        return positionQty;
    }
    
    public void setPositionQty(BigDecimal positionQty) {
        this.positionQty = positionQty;
    }
    
    public BigDecimal getAvgEntryPrice() {
        return avgEntryPrice;
    }
    
    public void setAvgEntryPrice(BigDecimal avgEntryPrice) {
        this.avgEntryPrice = avgEntryPrice;
    }
    
    public Instant getLastEventTimeUtc() {
        return lastEventTimeUtc;
    }
    
    public void setLastEventTimeUtc(Instant lastEventTimeUtc) {
        this.lastEventTimeUtc = lastEventTimeUtc;
    }
    
    public String getPersistedStateHash() {
        return persistedStateHash;
    }
    
    public void setPersistedStateHash(String persistedStateHash) {
        this.persistedStateHash = persistedStateHash;
    }
    
    /**
     * 计算状态哈希（用于判断是否需要持久化）
     * 
     * <p>基于 (phase, side, qty, avg) 四元组计算
     * 
     * @return MD5 哈希值
     */
    public String computeStateHash() {
        String stateString = String.format("%s|%s|%s|%s",
            phase != null ? phase.toString() : "NULL",
            positionSide != null ? positionSide : "NULL",
            positionQty != null ? positionQty.toString() : "NULL",
            avgEntryPrice != null ? avgEntryPrice.toString() : "NULL"
        );
        return String.valueOf(stateString.hashCode());  // 简化版，实际可用 MD5
    }
    
    /**
     * 判断状态是否发生变化（需要持久化）
     * 
     * <p>只有以下字段变化才需要写库：
     * - phase
     * - positionSide
     * - positionQty
     * - avgEntryPrice
     * 
     * @return true 表示有变化，需要持久化
     */
    public boolean hasChanged() {
        String currentHash = computeStateHash();
        return !Objects.equals(currentHash, persistedStateHash);
    }
    
    /**
     * 判断状态是否为空（未初始化）
     * 
     * @return true 表示未初始化
     */
    public boolean isEmpty() {
        return phase == null && positionSide == null && 
               positionQty == null && avgEntryPrice == null;
    }
}

