package com.qyl.v2trade.market.subscription.collector.websocket;

import com.qyl.v2trade.config.OkxWebSocketProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.ByteString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 价格订阅 WebSocket 管理器
 * 
 * <p>专门管理价格订阅（ticker频道）的WebSocket连接。
 * <p>使用端点：wss://ws.okx.com:8443/ws/v5/public
 * 
 * <p>职责：
 * <ul>
 *   <li>管理价格订阅的WebSocket连接（/ws/v5/public端点）</li>
 *   <li>处理价格订阅/取消订阅</li>
 *   <li>心跳和重连机制</li>
 * </ul>
 *
 * @author qyl
 */
@Slf4j
@Component
public class PriceWebSocketManager {

    @Autowired
    private OkxWebSocketProperties websocketProperties;

    @Autowired
    private com.qyl.v2trade.market.subscription.collector.router.ChannelRouter channelRouter;

    /**
     * WebSocket 连接
     */
    private okhttp3.WebSocket webSocket;

    /**
     * 连接状态
     */
    private final AtomicBoolean connected = new AtomicBoolean(false);

    /**
     * 关闭标志位（应用关闭时设置为 true，阻止新的重连尝试）
     */
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    /**
     * 初始化标志位（用于区分启动阶段和运行阶段）
     */
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * 期望的价格订阅列表（Source of Truth）
     */
    private final Set<String> desiredPriceSubscriptions = ConcurrentHashMap.newKeySet();

    /**
     * 重连控制
     */
    private volatile boolean reconnecting = false;
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private static final int MAX_RECONNECT_ATTEMPTS = 3;
    private static final long[] RECONNECT_DELAYS_MS = {5000, 10000, 20000}; // 5s, 10s, 20s

    /**
     * 心跳间隔（秒）
     */
    private static final int HEARTBEAT_INTERVAL_SECONDS = 30;

    /**
     * 静默检测间隔（秒，如果超过此时间没有收到任何消息，触发重连）
     */
    private static final int SILENCE_DETECTION_INTERVAL_SECONDS = 60;

    /**
     * OkHttp Client
     */
    private OkHttpClient httpClient;

    /**
     * 最后消息时间（用于静默检测）
     */
    private final AtomicLong lastMessageTime = new AtomicLong(0);

    /**
     * 心跳任务
     */
    private ScheduledFuture<?> heartbeatTask;

    /**
     * 静默检测任务
     */
    private ScheduledFuture<?> silenceDetectionTask;

    /**
     * 心跳线程池
     */
    private ScheduledExecutorService heartbeatExecutor;

    /**
     * 静默检测线程池
     */
    private ScheduledExecutorService silenceDetectorExecutor;

    /**
     * 重连线程池
     */
    private final ExecutorService reconnectExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "PriceWebSocket-Reconnect");
        t.setDaemon(true);
        return t;
    });

    /**
     * 初始化
     */
    @PostConstruct
    public void init() {
        if (initialized.get()) {
            log.warn("PriceWebSocketManager 已初始化，跳过");
            return;
        }

        log.info("PriceWebSocketManager 初始化开始...");

        // 检查配置
        validateConfiguration();

        // 构建 OkHttpClient
        buildHttpClient();

        initialized.set(true);
        log.info("PriceWebSocketManager 初始化完成");
    }

    /**
     * 验证配置
     */
    private void validateConfiguration() {
        if (websocketProperties == null) {
            throw new IllegalStateException("OkxWebSocketProperties 未配置");
        }

        // 价格订阅使用 /ws/v5/public 端点
        // 根据OKX API文档：ticker频道属于公共数据频道，应使用 /ws/v5/public 端点
        // /ws/v5/business 端点用于业务频道（如K线），不支持ticker订阅
        log.info("价格订阅将使用端点: wss://ws.okx.com:8443/ws/v5/public");
    }

    /**
     * 构建 OkHttpClient
     */
    private void buildHttpClient() {
        Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", 7890));
        OkHttpClient.Builder builder = new OkHttpClient.Builder().proxy(proxy)
                .connectTimeout(websocketProperties.getConnectTimeoutSeconds(), TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .writeTimeout(0, TimeUnit.SECONDS)
                .pingInterval(20, TimeUnit.SECONDS);

        this.httpClient = builder.build();
    }

    /**
     * 建立连接（异步，不阻塞）
     */
    public void connect() {
        if (shutdown.get()) {
            log.warn("应用已关闭，不再建立连接");
            return;
        }

        if (connected.get()) {
            log.warn("价格订阅WebSocket已连接，无需重复连接");
            return;
        }

        try {
            // 价格订阅使用 /ws/v5/public 端点
            // 根据OKX API文档：ticker频道属于公共数据频道，应使用 /ws/v5/public 端点
            String url = "wss://ws.okx.com:8443/ws/v5/public";
            log.info("尝试连接价格订阅WebSocket: {}", url);

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            webSocket = httpClient.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(okhttp3.WebSocket webSocket, Response response) {
                    connected.set(true);
                    reconnectAttempts.set(0);
                    lastMessageTime.set(System.currentTimeMillis());
                    log.info("价格订阅WebSocket 连接已建立: {}", url);

                    startHeartbeat();
                    startSilenceDetection();

                    // 重连成功后，恢复所有期望的订阅
                    if (!desiredPriceSubscriptions.isEmpty()) {
                        log.info("重连成功，恢复 {} 个价格订阅", desiredPriceSubscriptions.size());
                        subscribeAll();
                    }
                }

                @Override
                public void onMessage(okhttp3.WebSocket webSocket, String text) {
                    handleMessage(text);
                }

                @Override
                public void onMessage(okhttp3.WebSocket webSocket, ByteString bytes) {
                    handleMessage(bytes.utf8());
                }

                @Override
                public void onClosing(okhttp3.WebSocket webSocket, int code, String reason) {
                    log.warn("价格订阅WebSocket 正在关闭: code={}, reason={}", code, reason);
                }

                @Override
                public void onClosed(okhttp3.WebSocket webSocket, int code, String reason) {
                    connected.set(false);
                    stopHeartbeat();
                    stopSilenceDetection();
                    log.warn("价格订阅WebSocket 连接关闭: code={}, reason={}", code, reason);

                    if (!shutdown.get()) {
                        scheduleReconnect();
                    }
                }

                @Override
                public void onFailure(okhttp3.WebSocket webSocket, Throwable t, Response response) {
                    connected.set(false);
                    stopHeartbeat();
                    stopSilenceDetection();
                    log.error("价格订阅WebSocket 连接失败: {}", t.getMessage(), t);

                    if (!shutdown.get()) {
                        scheduleReconnect();
                    }
                }
            });

        } catch (Exception e) {
            log.error("价格订阅WebSocket 连接失败: {}", e.getMessage(), e);
            connected.set(false);
            reconnectAttempts.incrementAndGet();
            if (!shutdown.get()) {
                scheduleReconnect();
            }
        }
    }

    /**
     * 处理消息
     */
    private void handleMessage(String message) {
        lastMessageTime.set(System.currentTimeMillis());

        // 处理 pong 响应（心跳响应）
        if ("pong".equals(message)) {
            log.debug("收到 pong 响应（价格订阅），连接正常");
            return;
        }

        // 路由消息到 ChannelRouter（由 PriceChannel 处理）
        if (channelRouter != null) {
            log.debug("价格订阅收到消息，开始路由: messageLength={}", message.length());
            channelRouter.route(message);
        } else {
            log.error("ChannelRouter未注入，无法路由价格消息！");
        }
    }

    /**
     * 启动心跳
     */
    private void startHeartbeat() {
        stopHeartbeat();

        if (heartbeatExecutor == null) {
            heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "PriceWebSocket-Heartbeat");
                t.setDaemon(true);
                return t;
            });
        }

        heartbeatTask = heartbeatExecutor.scheduleAtFixedRate(() -> {
            try {
                if (connected.get() && webSocket != null) {
                    webSocket.send("ping");
                    log.debug("发送 ping 心跳（价格订阅）");
                }
            } catch (Exception e) {
                log.warn("发送 ping 心跳失败（价格订阅）: {}", e.getMessage());
            }
        }, HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);

        log.info("价格订阅心跳任务已启动，间隔 {} 秒", HEARTBEAT_INTERVAL_SECONDS);
    }

    /**
     * 停止心跳
     */
    private void stopHeartbeat() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
            heartbeatTask = null;
        }
    }

    /**
     * 启动静默检测
     */
    private void startSilenceDetection() {
        stopSilenceDetection();

        if (silenceDetectorExecutor == null) {
            silenceDetectorExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "PriceWebSocket-SilenceDetector");
                t.setDaemon(true);
                return t;
            });
        }

        silenceDetectionTask = silenceDetectorExecutor.scheduleAtFixedRate(() -> {
            try {
                long now = System.currentTimeMillis();
                long lastMsgTime = lastMessageTime.get();
                long silenceDuration = now - lastMsgTime;

                if (lastMsgTime > 0 && silenceDuration > SILENCE_DETECTION_INTERVAL_SECONDS * 1000L) {
                    log.warn("价格订阅WebSocket 静默时间过长（{} 秒），触发重连", silenceDuration / 1000);
                    if (connected.get() && !reconnecting) {
                        reconnect();
                    }
                }
            } catch (Exception e) {
                log.error("静默检测异常（价格订阅）", e);
            }
        }, SILENCE_DETECTION_INTERVAL_SECONDS, SILENCE_DETECTION_INTERVAL_SECONDS, TimeUnit.SECONDS);

        log.info("价格订阅静默检测任务已启动，间隔 {} 秒", SILENCE_DETECTION_INTERVAL_SECONDS);
    }

    /**
     * 停止静默检测
     */
    private void stopSilenceDetection() {
        if (silenceDetectionTask != null) {
            silenceDetectionTask.cancel(false);
            silenceDetectionTask = null;
        }
    }

    /**
     * 安排重连
     */
    private void scheduleReconnect() {
        if (reconnecting) {
            log.debug("价格订阅WebSocket 已在重连中，跳过");
            return;
        }

        if (shutdown.get()) {
            log.debug("应用已关闭，取消价格订阅WebSocket重连");
            return;
        }

        int attempts = reconnectAttempts.get();
        if (attempts >= MAX_RECONNECT_ATTEMPTS) {
            log.error("价格订阅WebSocket 重连次数已达上限（{} 次），停止重连", MAX_RECONNECT_ATTEMPTS);
            return;
        }

        reconnecting = true;

        long delay = attempts < RECONNECT_DELAYS_MS.length 
                ? RECONNECT_DELAYS_MS[attempts] 
                : RECONNECT_DELAYS_MS[RECONNECT_DELAYS_MS.length - 1];

        reconnectExecutor.execute(() -> {
            try {
                log.info("等待 {} 秒后开始重连价格订阅WebSocket (尝试 {}/{})...", 
                        delay / 1000, attempts + 1, MAX_RECONNECT_ATTEMPTS);
                Thread.sleep(delay);

                if (shutdown.get()) {
                    reconnecting = false;
                    return;
                }

                reconnect();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("重连等待被中断（价格订阅）");
                reconnecting = false;
            } catch (Exception e) {
                log.error("安排重连时发生异常（价格订阅）", e);
                reconnecting = false;
            }
        });
    }

    /**
     * 重连
     */
    private void reconnect() {
        if (shutdown.get()) {
            reconnecting = false;
            return;
        }

        try {
            if (webSocket != null) {
                try {
                    webSocket.close(1000, "Reconnecting");
                } catch (Exception e) {
                    log.warn("关闭旧连接失败（价格订阅）", e);
                }
                webSocket = null;
            }

            connect();

        } catch (Exception e) {
            log.error("价格订阅WebSocket 重连失败: {}", e.getMessage(), e);
            reconnectAttempts.incrementAndGet();
            reconnecting = false;

            if (reconnectAttempts.get() < MAX_RECONNECT_ATTEMPTS) {
                scheduleReconnect();
            } else {
                log.error("价格订阅WebSocket 重连次数已达上限，停止重连");
            }
        }
    }

    /**
     * 订阅价格（ticker频道）- 单个交易对
     * 
     * @param symbol 交易对符号（如："BTC-USDT-SWAP"）
     */
    public void subscribePrice(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            log.warn("订阅价格符号为空");
            return;
        }
        subscribePrice(Set.of(symbol));
    }

    /**
     * 订阅价格（ticker频道）- 批量订阅
     * 
     * <p>支持单个或批量订阅，自动兼容：
     * <ul>
     *   <li>单个订阅：Set包含1个元素时，生成单个args的订阅消息</li>
     *   <li>批量订阅：Set包含多个元素时，生成多个args的订阅消息，在同一个subscribe请求中发送</li>
     * </ul>
     * 
     * <p>OKX WebSocket API支持在一个subscribe消息中同时订阅多个ticker频道，格式示例：
     * <pre>
     * // 单个订阅
     * {"op":"subscribe","args":[{"channel":"ticker","instId":"BTC-USDT-SWAP"}]}
     * 
     * // 批量订阅
     * {"op":"subscribe","args":[
     *   {"channel":"ticker","instId":"BTC-USDT-SWAP"},
     *   {"channel":"ticker","instId":"ETH-USDT-SWAP"}
     * ]}
     * </pre>
     * 
     * @param symbols 交易对符号集合（如：["BTC-USDT-SWAP", "ETH-USDT-SWAP"]）
     */
    public void subscribePrice(Set<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            log.warn("订阅价格符号集合为空");
            return;
        }

        desiredPriceSubscriptions.addAll(symbols);
        log.info("更新期望价格订阅列表，当前订阅数: {}，本次订阅: {}", 
                desiredPriceSubscriptions.size(), symbols.size());

        if (connected.get() && webSocket != null) {
            sendPriceSubscribeMessage(symbols);
        } else {
            log.debug("价格订阅WebSocket 未连接，订阅将在连接建立后自动恢复");
        }
    }

    /**
     * 取消订阅价格 - 单个交易对
     * 
     * @param symbol 交易对符号
     */
    public void unsubscribePrice(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            log.warn("取消订阅价格符号为空");
            return;
        }
        unsubscribePrice(Set.of(symbol));
    }

    /**
     * 取消订阅价格 - 批量取消订阅
     * 
     * <p>支持单个或批量取消订阅，自动兼容：
     * <ul>
     *   <li>单个取消：Set包含1个元素时，生成单个args的取消订阅消息</li>
     *   <li>批量取消：Set包含多个元素时，生成多个args的取消订阅消息，在同一个unsubscribe请求中发送</li>
     * </ul>
     * 
     * @param symbols 交易对符号集合
     */
    public void unsubscribePrice(Set<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            log.warn("取消订阅价格符号集合为空");
            return;
        }

        desiredPriceSubscriptions.removeAll(symbols);
        log.info("更新期望价格订阅列表，当前订阅数: {}，本次取消: {}", 
                desiredPriceSubscriptions.size(), symbols.size());

        if (connected.get() && webSocket != null) {
            sendPriceUnsubscribeMessage(symbols);
        }
    }

    /**
     * 订阅所有已注册的价格订阅
     */
    public void subscribeAll() {
        if (!connected.get() || webSocket == null) {
            log.warn("价格订阅WebSocket 未连接，无法订阅");
            return;
        }

        if (!desiredPriceSubscriptions.isEmpty()) {
            sendPriceSubscribeMessage(desiredPriceSubscriptions);
        } else {
            log.info("期望价格订阅列表为空，无需订阅");
        }
    }

    /**
     * 发送价格订阅消息（ticker频道）
     * 
     * <p><b>单个和批量订阅完全兼容：</b>
     * <ul>
     *   <li>单个订阅：symbols.size() == 1，生成单个args的订阅消息</li>
     *   <li>批量订阅：symbols.size() > 1，生成多个args的订阅消息，在同一个subscribe请求中发送</li>
     * </ul>
     * 
     * <p><b>频道名称说明：</b>
     * 根据OKX API文档，订阅特定交易对的ticker数据时，统一使用 <code>"ticker"</code>（单数）频道名称。
     * 无论是单个订阅还是批量订阅，每个args中的channel都是 <code>"ticker"</code>。
     * 
     * <p><b>生成的JSON格式示例：</b>
     * <pre>
     * // 单个订阅（symbols包含1个元素）
     * {"op":"subscribe","args":[{"channel":"ticker","instId":"BTC-USDT-SWAP"}]}
     * 
     * // 批量订阅（symbols包含多个元素）
     * {"op":"subscribe","args":[
     *   {"channel":"ticker","instId":"BTC-USDT-SWAP"},
     *   {"channel":"ticker","instId":"ETH-USDT-SWAP"}
     * ]}
     * </pre>
     * 
     * @param symbols 交易对符号集合（1个或多个）
     */
    private void sendPriceSubscribeMessage(Set<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            log.warn("发送订阅消息：符号集合为空");
            return;
        }

        StringBuilder args = new StringBuilder();
        for (String symbol : symbols) {
            if (args.length() > 0) {
                args.append(",");
            }
            // 统一使用 "ticker"（单数）频道，无论单个还是批量订阅
            args.append(String.format("{\"channel\":\"tickers\",\"instId\":\"%s\"}", symbol));
        }

        String message = String.format("{\"op\":\"subscribe\",\"args\":[%s]}", args.toString());
        log.debug("生成订阅消息: symbols={}, message={}", symbols.size(), message);
        sendWebSocketMessage(message, symbols, "价格订阅");
    }

    /**
     * 发送取消价格订阅消息
     * 
     * <p><b>单个和批量取消订阅完全兼容：</b>
     * 格式与订阅消息相同，统一使用 <code>"ticker"</code>（单数）频道名称。
     * 
     * @param symbols 交易对符号集合（1个或多个）
     */
    private void sendPriceUnsubscribeMessage(Set<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            log.warn("发送取消订阅消息：符号集合为空");
            return;
        }

        StringBuilder args = new StringBuilder();
        for (String symbol : symbols) {
            if (args.length() > 0) {
                args.append(",");
            }
            // 统一使用 "ticker"（单数）频道，无论单个还是批量取消订阅
            args.append(String.format("{\"channel\":\"tickers\",\"instId\":\"%s\"}", symbol));
        }

        String message = String.format("{\"op\":\"unsubscribe\",\"args\":[%s]}", args.toString());
        log.debug("生成取消订阅消息: symbols={}, message={}", symbols.size(), message);
        sendWebSocketMessage(message, symbols, "取消价格订阅");
    }

    /**
     * 发送WebSocket消息（通用方法）
     */
    private void sendWebSocketMessage(String message, Set<String> symbols, String type) {
        try {
            if (webSocket != null) {
                boolean sent = webSocket.send(message);
                if (sent) {
                    log.info("发送{}消息成功: symbols={}", type, symbols);
                } else {
                    log.error("发送{}消息失败：WebSocket 可能已关闭", type);
                }
            }
        } catch (Exception e) {
            log.error("发送{}消息异常: symbols={}", type, symbols, e);
        }
    }

    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        return connected.get();
    }

    /**
     * 优雅关闭
     */
    @PreDestroy
    public void shutdown() {
        log.info("开始关闭 PriceWebSocketManager...");

        shutdown.set(true);

        stopHeartbeat();
        stopSilenceDetection();

        if (webSocket != null) {
            try {
                webSocket.close(1000, "Application shutdown");
            } catch (Exception e) {
                log.warn("关闭价格订阅WebSocket失败", e);
            }
            webSocket = null;
        }

        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdown();
            try {
                if (!heartbeatExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    heartbeatExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                heartbeatExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (silenceDetectorExecutor != null) {
            silenceDetectorExecutor.shutdown();
            try {
                if (!silenceDetectorExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    silenceDetectorExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                silenceDetectorExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        reconnectExecutor.shutdown();
        try {
            if (!reconnectExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                reconnectExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            reconnectExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("PriceWebSocketManager 已关闭");
    }
}

