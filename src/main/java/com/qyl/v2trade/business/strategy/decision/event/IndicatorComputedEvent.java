package com.qyl.v2trade.business.strategy.decision.event;

import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 指标计算完成事件
 * 
 * <p>当指标值写入indicator_value表成功后发布此事件
 * 
 * <p>用于触发策略决策（指标驱动策略）
 * 
 * <p>路由规则：
 * <ul>
 *   <li>根据tradingPairId查找所有订阅该指标的策略实例</li>
 *   <li>需要查询strategy_signal_subscription表（如果策略订阅了该指标）</li>
 *   <li>如果找不到，记录DEBUG日志（正常情况，不是所有策略都订阅所有指标）</li>
 * </ul>
 */
public class IndicatorComputedEvent extends ApplicationEvent {

    /**
     * 用户ID
     */
    private final Long userId;

    /**
     * 交易对ID
     */
    private final Long tradingPairId;

    /**
     * 指标代码（如：RSI_14, MACD）
     */
    private final String indicatorCode;

    /**
     * 指标版本
     */
    private final String indicatorVersion;

    /**
     * K线时间（指标计算的bar时间）
     */
    private final LocalDateTime barTime;

    /**
     * 指标值（Map格式，可能包含多个值）
     * 
     * <p>示例：
     * <ul>
     *   <li>RSI: {"RSI": 30.5}</li>
     *   <li>MACD: {"MACD": 100.0, "SIGNAL": 95.0, "HIST": 5.0}</li>
     * </ul>
     */
    private final Map<String, BigDecimal> indicatorValues;

    /**
     * 计算完成时间
     */
    private final LocalDateTime computedAt;

    /**
     * 构造函数
     * 
     * @param source 事件源
     * @param userId 用户ID
     * @param tradingPairId 交易对ID
     * @param indicatorCode 指标代码
     * @param indicatorVersion 指标版本
     * @param barTime K线时间
     * @param indicatorValues 指标值
     * @param computedAt 计算完成时间
     */
    public IndicatorComputedEvent(Object source, Long userId, Long tradingPairId,
                                  String indicatorCode, String indicatorVersion,
                                  LocalDateTime barTime, Map<String, BigDecimal> indicatorValues,
                                  LocalDateTime computedAt) {
        super(source);
        this.userId = userId;
        this.tradingPairId = tradingPairId;
        this.indicatorCode = indicatorCode;
        this.indicatorVersion = indicatorVersion;
        this.barTime = barTime;
        this.indicatorValues = indicatorValues;
        this.computedAt = computedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getTradingPairId() {
        return tradingPairId;
    }

    public String getIndicatorCode() {
        return indicatorCode;
    }

    public String getIndicatorVersion() {
        return indicatorVersion;
    }

    public LocalDateTime getBarTime() {
        return barTime;
    }

    public Map<String, BigDecimal> getIndicatorValues() {
        return indicatorValues;
    }

    public LocalDateTime getComputedAt() {
        return computedAt;
    }

    @Override
    public String toString() {
        return "IndicatorComputedEvent{" +
                "userId=" + userId +
                ", tradingPairId=" + tradingPairId +
                ", indicatorCode='" + indicatorCode + '\'' +
                ", indicatorVersion='" + indicatorVersion + '\'' +
                ", barTime=" + barTime +
                ", indicatorValues=" + indicatorValues +
                ", computedAt=" + computedAt +
                '}';
    }
}

