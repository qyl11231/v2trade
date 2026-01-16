package com.qyl.v2trade.business.strategy.runtime.trigger;

/**
 * 事件键构建器
 * 
 * <p>用于生成全局唯一、可复现的事件键（eventKey），用于幂等去重。
 * 
 * <p>【重要】eventKey 规范一旦确定，后续不能修改（N3/N5/N6 都依赖它）
 *
 * @author qyl
 */
public class EventKeyBuilder {
    
    /**
     * 构建 BAR_CLOSE 事件键
     * 
     * <p>格式：BAR:{pairId}:{timeframe}:{barCloseEpochMillis}
     * 
     * @param pairId 交易对ID
     * @param timeframe 时间周期（如 "5m", "1h"）
     * @param barCloseEpochMillis K线闭合时间戳（毫秒，UTC）
     * @return 事件键
     */
    public static String buildBarCloseKey(Long pairId, String timeframe, long barCloseEpochMillis) {
        return String.format("BAR:%d:%s:%d", pairId, timeframe, barCloseEpochMillis);
    }
    
    /**
     * 构建 PRICE 事件键
     * 
     * <p>格式：PRICE:{pairId}:{priceEpochMillis}
     * 
     * <p>注意：若没有 seq，用毫秒时间；高频时会更"稀疏去重"，可接受（N2 只打印）
     * 
     * @param pairId 交易对ID
     * @param priceEpochMillis 价格时间戳（毫秒，UTC）
     * @return 事件键
     */
    public static String buildPriceKey(Long pairId, long priceEpochMillis) {
        return String.format("PRICE:%d:%d", pairId, priceEpochMillis);
    }
    
    /**
     * 构建 SIGNAL 事件键
     * 
     * <p>格式：SIGNAL:{signalConfigId}:{signalId}
     * 
     * <p>注意：signalId 必须唯一；没有则退化为 hash(payload)
     * 
     * @param signalConfigId 信号配置ID
     * @param signalId 信号ID（必须唯一）
     * @return 事件键
     */
    public static String buildSignalKey(Long signalConfigId, String signalId) {
        return String.format("SIGNAL:%d:%s", signalConfigId, signalId);
    }
}

