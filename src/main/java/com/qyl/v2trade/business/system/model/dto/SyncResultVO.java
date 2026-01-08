package com.qyl.v2trade.business.system.model.dto;

import lombok.Data;

/**
 * 同步结果VO
 */
@Data
public class SyncResultVO {
    
    /**
     * 市场类型
     */
    private String marketType;
    
    /**
     * 同步数量
     */
    private Integer syncCount;
    
    /**
     * 同步时间（毫秒）
     */
    private Long syncTime;
    
    public SyncResultVO() {}
    
    public SyncResultVO(String marketType, Integer syncCount, Long syncTime) {
        this.marketType = marketType;
        this.syncCount = syncCount;
        this.syncTime = syncTime;
    }
}

