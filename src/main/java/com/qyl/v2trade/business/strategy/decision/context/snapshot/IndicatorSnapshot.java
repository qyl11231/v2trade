package com.qyl.v2trade.business.strategy.decision.context.snapshot;

import lombok.Builder;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 指标快照（不可变）
 * 
 * <p>从 indicator_value 表读取的指标快照（最新值）
 * 
 * <p>用于指标驱动策略的决策
 */
@Getter
@Builder
public class IndicatorSnapshot implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 指标代码（如：RSI_14, MACD）
     */
    private final String indicatorCode;

    /**
     * 指标版本
     */
    private final String indicatorVersion;

    /**
     * K线周期（如：1m, 5m, 15m, 1h）
     */
    private final String timeframe;

    /**
     * K线时间（指标计算的bar时间）
     */
    private final LocalDateTime barTime;

    /**
     * 指标主值
     */
    private final BigDecimal value;

    /**
     * 指标扩展值（Map格式，可能包含多个值）
     * 
     * <p>示例：
     * <ul>
     *   <li>RSI: null（只有value）</li>
     *   <li>MACD: {"MACD": 100.0, "SIGNAL": 95.0, "HIST": 5.0}</li>
     * </ul>
     */
    private final Map<String, BigDecimal> extraValues;

    /**
     * 数据质量标记
     */
    private final String dataQuality;

    /**
     * 计算完成时间
     */
    private final LocalDateTime computedAt;

    /**
     * 判断指标值是否存在
     */
    public boolean hasValue() {
        return value != null;
    }

    /**
     * 获取指定扩展值
     * 
     * @param key 扩展值key
     * @return 扩展值，如果不存在返回null
     */
    public BigDecimal getExtraValue(String key) {
        if (extraValues == null || key == null) {
            return null;
        }
        return extraValues.get(key);
    }
}

