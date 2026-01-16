package com.qyl.v2trade.business.strategy.runtime.snapshot;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 最新信号快照
 * 
 * <p>线程安全，只保存最新信号数据
 *
 * @author qyl
 */
public class LatestSignalSnapshot {
    
    private volatile Long signalConfigId;
    private volatile String signalId;
    private volatile String signalDirectionHint;  // LONG/SHORT/NEUTRAL
    private volatile BigDecimal price;
    private volatile Instant receivedTimeUtc;
    
    /**
     * 线程安全的更新方法
     * 
     * <p>只更新时间不早于当前快照的数据
     * 
     * @param configId 信号配置ID
     * @param sigId 信号ID
     * @param direction 信号方向提示
     * @param price 信号价格
     * @param receivedTime 接收时间（UTC）
     */
    public synchronized void update(Long configId, String sigId, String direction, 
                                   BigDecimal price, Instant receivedTime) {
        if (configId == null || sigId == null || receivedTime == null) {
            return;
        }
        
        // 只更新时间不早于当前快照的数据
        if (receivedTimeUtc == null || !receivedTime.isBefore(receivedTimeUtc)) {
            this.signalConfigId = configId;
            this.signalId = sigId;
            this.signalDirectionHint = direction;
            this.price = price;
            this.receivedTimeUtc = receivedTime;
        }
    }
    
    // Getters
    
    public Long getSignalConfigId() {
        return signalConfigId;
    }
    
    public String getSignalId() {
        return signalId;
    }
    
    public String getSignalDirectionHint() {
        return signalDirectionHint;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public Instant getReceivedTimeUtc() {
        return receivedTimeUtc;
    }
    
    /**
     * 判断快照是否为空
     * 
     * @return true 表示未初始化
     */
    public boolean isEmpty() {
        return signalConfigId == null || signalId == null || receivedTimeUtc == null;
    }
}

