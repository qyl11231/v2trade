package com.qyl.v2trade.market.model;

import com.qyl.v2trade.common.util.TimeUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * 标准化K线数据模型
 * 统一所有交易所的K线格式
 * 
 * <p>时间字段统一使用 {@link Instant} 类型，表示 UTC 时间点。
 * 
 * <p><b>重构：按照时间管理约定</b>
 * <ul>
 *   <li>内部存储为 {@link Instant} 类型（UTC）</li>
 *   <li>推荐使用 {@link #getTimestampInstant()} 和 {@link #setTimestampInstant(Instant)} 方法</li>
 *   <li>Builder 模式：使用 {@code .timestamp(Instant)} 和 {@code .exchangeTimestamp(Instant)}</li>
 *   <li>兼容性方法：保留 {@link #getTimestamp()} 和 {@link #setTimestamp(Long)}，用于数据库边界和旧代码</li>
 *   <li>所有转换都使用 {@link TimeUtil} 保持一致性</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NormalizedKline implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 交易对符号（标准化格式，如：BTC-USDT）
     */
    private String symbol;

    /**
     * K线周期（如：1m, 5m, 15m）
     */
    private String interval;

    /**
     * 开盘价
     */
    private Double open;

    /**
     * 最高价
     */
    private Double high;

    /**
     * 最低价
     */
    private Double low;

    /**
     * 收盘价
     */
    private Double close;

    /**
     * 成交量
     */
    private Double volume;

    /**
     * K线开盘时间（UTC）
     * 
     * <p>内部存储为 Instant，对外提供 Long (epoch millis) 的 getter/setter 以保持兼容性
     */
    private Instant timestamp;

    /**
     * 交易所原始时间戳（UTC，用于去重）
     * 
     * <p>内部存储为 Instant，对外提供 Long (epoch millis) 的 getter/setter 以保持兼容性
     */
    private Instant exchangeTimestamp;

    // ==================== 推荐方法：直接使用 Instant ====================

    /**
     * 获取 K线开盘时间（UTC）
     * 
     * <p>推荐使用此方法，直接返回 Instant 对象
     * 
     * @return Instant 对象，如果 timestamp 为 null 则返回 null
     */
    public Instant getTimestampInstant() {
        return timestamp;
    }

    /**
     * 设置 K线开盘时间（UTC）
     * 
     * <p>推荐使用此方法，直接接收 Instant 对象
     * 
     * @param instant Instant 对象
     */
    public void setTimestampInstant(Instant instant) {
        this.timestamp = instant;
    }

    /**
     * 获取交易所原始时间戳（UTC）
     * 
     * <p>推荐使用此方法，直接返回 Instant 对象
     * 
     * @return Instant 对象，如果 exchangeTimestamp 为 null 则返回 null
     */
    public Instant getExchangeTimestampInstant() {
        return exchangeTimestamp;
    }

    /**
     * 设置交易所原始时间戳（UTC）
     * 
     * <p>推荐使用此方法，直接接收 Instant 对象
     * 
     * @param instant Instant 对象
     */
    public void setExchangeTimestampInstant(Instant instant) {
        this.exchangeTimestamp = instant;
    }

    // ==================== 兼容性方法：Long <-> Instant ====================

    /**
     * 获取时间戳（毫秒级，兼容旧代码）
     * 
     * <p>重构：使用 TimeUtil 进行转换，保持转换逻辑的一致性
     * 
     * <p><b>注意：此方法用于兼容性，建议新代码使用 {@link #getTimestampInstant()}</b>
     * 
     * @return UTC 毫秒时间戳，如果 timestamp 为 null 则返回 null
     */
    public Long getTimestamp() {
        return timestamp != null ? TimeUtil.toEpochMilli(timestamp) : null;
    }

    /**
     * 设置时间戳（毫秒级，兼容旧代码）
     * 
     * <p>重构：使用 TimeUtil 进行转换，保持转换逻辑的一致性
     * 
     * <p><b>注意：此方法用于兼容性，建议新代码使用 {@link #setTimestampInstant(Instant)} 或 Builder 的 {@code .timestamp(Instant)}</b>
     * 
     * @param epochMilli UTC 毫秒时间戳
     */
    public void setTimestamp(Long epochMilli) {
        this.timestamp = epochMilli != null ? TimeUtil.fromEpochMilli(epochMilli) : null;
    }

    /**
     * 获取交易所原始时间戳（毫秒级，兼容旧代码）
     * 
     * <p>重构：使用 TimeUtil 进行转换，保持转换逻辑的一致性
     * 
     * <p><b>注意：此方法用于兼容性，建议新代码使用 {@link #getExchangeTimestampInstant()}</b>
     * 
     * @return UTC 毫秒时间戳，如果 exchangeTimestamp 为 null 则返回 null
     */
    public Long getExchangeTimestamp() {
        return exchangeTimestamp != null ? TimeUtil.toEpochMilli(exchangeTimestamp) : null;
    }

    /**
     * 设置交易所原始时间戳（毫秒级，兼容旧代码）
     * 
     * <p>重构：使用 TimeUtil 进行转换，保持转换逻辑的一致性
     * 
     * <p><b>注意：此方法用于兼容性，建议新代码使用 {@link #setExchangeTimestampInstant(Instant)} 或 Builder 的 {@code .exchangeTimestamp(Instant)}</b>
     * 
     * @param epochMilli UTC 毫秒时间戳
     */
    public void setExchangeTimestamp(Long epochMilli) {
        this.exchangeTimestamp = epochMilli != null ? TimeUtil.fromEpochMilli(epochMilli) : null;
    }
}

