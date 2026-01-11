package com.qyl.v2trade.market.model;

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
 * <p>兼容性说明：
 * <ul>
 *   <li>为了保持与现有代码的兼容性，保留了 getTimestamp() 和 setTimestamp() 方法（返回/接收 Long）</li>
 *   <li>建议新代码使用 getTimestamp() 和 setTimestamp() 方法，内部自动转换</li>
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

    // ==================== 兼容性方法：Long <-> Instant ====================

    /**
     * 获取时间戳（毫秒级，兼容旧代码）
     * 
     * @return UTC 毫秒时间戳，如果 timestamp 为 null 则返回 null
     */
    public Long getTimestamp() {
        return timestamp != null ? timestamp.toEpochMilli() : null;
    }

    /**
     * 设置时间戳（毫秒级，兼容旧代码）
     * 
     * @param epochMilli UTC 毫秒时间戳
     */
    public void setTimestamp(Long epochMilli) {
        this.timestamp = epochMilli != null ? Instant.ofEpochMilli(epochMilli) : null;
    }

    /**
     * 获取交易所原始时间戳（毫秒级，兼容旧代码）
     * 
     * @return UTC 毫秒时间戳，如果 exchangeTimestamp 为 null 则返回 null
     */
    public Long getExchangeTimestamp() {
        return exchangeTimestamp != null ? exchangeTimestamp.toEpochMilli() : null;
    }

    /**
     * 设置交易所原始时间戳（毫秒级，兼容旧代码）
     * 
     * @param epochMilli UTC 毫秒时间戳
     */
    public void setExchangeTimestamp(Long epochMilli) {
        this.exchangeTimestamp = epochMilli != null ? Instant.ofEpochMilli(epochMilli) : null;
    }

    // ==================== 新增方法：直接使用 Instant ====================

    /**
     * 获取 K线开盘时间（UTC）
     * 
     * @return Instant 对象
     */
    public Instant getTimestampInstant() {
        return timestamp;
    }

    /**
     * 设置 K线开盘时间（UTC）
     * 
     * @param instant Instant 对象
     */
    public void setTimestampInstant(Instant instant) {
        this.timestamp = instant;
    }

    /**
     * 获取交易所原始时间戳（UTC）
     * 
     * @return Instant 对象
     */
    public Instant getExchangeTimestampInstant() {
        return exchangeTimestamp;
    }

    /**
     * 设置交易所原始时间戳（UTC）
     * 
     * @param instant Instant 对象
     */
    public void setExchangeTimestampInstant(Instant instant) {
        this.exchangeTimestamp = instant;
    }
}

