package com.qyl.v2trade.business.model.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * TradingView Webhook 请求DTO
 * 
 * TradingView 标准格式参考：
 * {
 *   "signal_config_id": 1,
 *   "signal_name": "kong-wu",
 *   "symbol": "{{ticker}}",
 *   "action": "{{strategy.order.action}}",
 *   "price": {{strategy.order.price}},
 *   "quantity": {{strategy.order.contracts}},
 *   "timeframe": "{{interval}}",
 *   "signalId": "{{strategy.order.id}}",
 *   "timestamp": "{{timenow}}"
 * }
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TradingViewWebhookRequest {

    /**
     * 信号配置ID（用于路由到对应的信号配置）
     */
    @JsonProperty("signal_config_id")
    private Long signalConfigId;

    /**
     * 信号名称
     */
    @JsonProperty("signal_name")
    private String signalName;

    /**
     * 交易对，如 BTCUSDT, ETHUSDT
     */
    private String symbol;

    /**
     * 操作类型：buy / sell / close
     */
    private String action;

    /**
     * 价格
     */
    private String price;

    /**
     * 数量
     */
    private String quantity;

    /**
     * 数量（别名，某些TradingView版本使用qty）
     */
    private String qty;

    /**
     * 时间周期，如 1m, 5m, 1h, 4h, 1d
     */
    private String timeframe;

    /**
     * TradingView策略订单ID
     */
    private String signalId;

    /**
     * 时间戳（TradingView格式，如 "2023-12-17T10:30:00Z"）
     */
    private String timestamp;

    /**
     * 策略名称（兼容旧格式）
     */
    @JsonProperty("strategy_name")
    private String strategyName;
}
