package com.qyl.v2trade.market.model.dto;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * K线查询响应DTO
 * 
 * <p>时间字段说明：
 * <ul>
 *   <li>timestamp: Instant 类型（UTC），用于内部传递和 JSON 序列化</li>
 *   <li>time: String 类型（上海时区），用于前端展示，在 Controller 层通过 TimeUtil.formatAsShanghaiString() 转换</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KlineResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 交易对符号
     */
    private String symbol;

    /**
     * K线周期
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
     * <p>内部使用 Instant 类型，JSON 序列化时通过 getTimestamp() 方法返回毫秒级时间戳
     */
    @JsonIgnore
    private Instant timestamp;
    
    /**
     * 获取时间戳（毫秒级），用于 JSON 序列化
     * 这个方法会被 Jackson 用于序列化 timestamp 字段，返回毫秒级时间戳
     */
    @JsonGetter("timestamp")
    public Long getTimestamp() {
        return timestamp != null ? timestamp.toEpochMilli() : null;
    }
    
    /**
     * 设置时间戳（从毫秒级时间戳设置）
     * 用于反序列化和 Builder 模式
     */
    public void setTimestamp(Long epochMilli) {
        this.timestamp = epochMilli != null ? Instant.ofEpochMilli(epochMilli) : null;
    }

    /**
     * 时间字符串（格式化后的时间，用于前端直接显示）
     * 格式：yyyy-MM-dd HH:mm:ss（上海时区，UTC+8）
     * 
     * <p>此字段在 Controller 层通过 TimeUtil.formatAsShanghaiString(timestamp) 设置
     */
    private String time;
}

