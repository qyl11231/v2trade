package com.qyl.v2trade.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OKX WebSocket 配置属性
 *
 * @author qyl
 */
@Data
@Component
@ConfigurationProperties(prefix = "okx.websocket")
public class OkxWebSocketProperties {

    /**
     * WebSocket URL
     * K线频道已迁移到 /business 端点（2023年4月24日后）
     * 公共频道：wss://ws.okx.com:8443/ws/v5/public
     * K线频道（business）：wss://ws.okx.com:8443/ws/v5/business
     */
    private String url = "wss://ws.okx.com:8443/ws/v5/business";

    /**
     * 连接超时时间（秒）
     */
    private int connectTimeoutSeconds = 15;

    /**
     * 是否启用代理
     */
    private boolean proxyEnabled = false;

    /**
     * 代理类型：http, socks
     */
    private String proxyType = "http";

    /**
     * 代理主机
     */
    private String proxyHost = "";

    /**
     * 代理端口
     */
    private int proxyPort = 0;

    /**
     * 代理用户名
     */
    private String proxyUsername = "";

    /**
     * 代理密码
     */
    private String proxyPassword = "";


}

