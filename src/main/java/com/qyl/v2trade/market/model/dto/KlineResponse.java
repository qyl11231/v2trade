package com.qyl.v2trade.market.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
     * <p>JSON 序列化时会自动转换为 epoch millis (long)
     * <p>使用 @JsonFormat 注解确保序列化为数字时间戳，而不是 ISO-8601 字符串
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Instant timestamp;

    /**
     * 时间字符串（格式化后的时间，用于前端直接显示）
     * 格式：yyyy-MM-dd HH:mm:ss（上海时区，UTC+8）
     * 
     * <p>此字段在 Controller 层通过 TimeUtil.formatAsShanghaiString(timestamp) 设置
     */
    private String time;
}

