package com.qyl.v2trade.market.subscription.collector.websocket;

import com.qyl.v2trade.config.OkxWebSocketProperties;
import com.qyl.v2trade.market.subscription.collector.router.ChannelRouter;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.ByteString;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 交易所 WebSocket 管理器
 * 
 * <p>负责管理 WebSocket 连接的生命周期，包括：
 * <ul>
 *   <li>连接建立和维护</li>
 *   <li>心跳检测（Ping-Pong）</li>
 *   <li>静默检测（60秒无消息强制重连）</li>
 *   <li>指数退避重连</li>
 *   <li>动态订阅管理</li>
 * </ul>
 *
 * @author qyl
 */
@Slf4j
public class ExchangeWebSocketManager {

    @Autowired
    private OkxWebSocketProperties websocketProperties;

    @Autowired
    private ChannelRouter channelRouter;

    /**
     * OkHttp WebSocket 客户端
     */
    private OkHttpClient httpClient;

    /**
     * WebSocket 连接实例
     */
    private volatile okhttp3.WebSocket webSocket;

    /**
     * 连接状态
     */
    private final AtomicBoolean connected = new AtomicBoolean(false);

    /**
     * 关闭标志位（应用关闭时设置为 true，阻止新的重连尝试）
     * 
     * <p>注意：只在 @PreDestroy 方法中被设置为 true，启动过程中的超时或异常不会设置此标志。
     */
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    
    /**
     * 初始化标志位（用于区分启动阶段和运行阶段）
     */
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * 期望的订阅列表（Source of Truth）
     * 
     * <p>所有 subscribe/unsubscribe 操作仅更新这个 Set，然后异步尝试发送 WS 指令。
     * 重连时，直接遍历这个 Set 进行重新订阅，确保最终状态一致。
     */
    private final Set<String> desiredSubscriptions = ConcurrentHashMap.newKeySet();

    /**
     * 重连控制
     */
    private volatile boolean reconnecting = false;
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private static final int MAX_RECONNECT_ATTEMPTS = 3;
    private static final long[] RECONNECT_DELAYS_MS = {5000, 10000, 20000}; // 5s, 10s, 20s

    /**
     * 心跳控制
     */
    private ScheduledExecutorService heartbeatExecutor;
    private ScheduledFuture<?> heartbeatTask;
    private static final long HEARTBEAT_INTERVAL_SECONDS = 20; // 每20秒发送一次ping

    /**
     * 静默检测
     */
    private final AtomicLong lastMessageTime = new AtomicLong(System.currentTimeMillis());
    private ScheduledExecutorService silenceDetectorExecutor;
    private ScheduledFuture<?> silenceDetectorTask;
    private static final long SILENCE_DETECTION_INTERVAL_SECONDS = 10; // 每10秒检查一次
    private static final long SILENCE_THRESHOLD_MS = 60000; // 60秒无消息则判定为僵尸连接

    /**
     * 重连线程池
     */
    private final ExecutorService reconnectExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ExchangeWebSocket-Reconnect");
        t.setDaemon(true);
        return t;
    });

    /**
     * 初始化
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        // 检查配置
        validateConfiguration();

        // 构建 OkHttpClient
        buildHttpClient();

        initialized.set(true);
        log.info("ExchangeWebSocketManager 初始化完成");
    }

    /**
     * 验证配置
     */
    private void validateConfiguration() {
        if (websocketProperties == null) {
            throw new IllegalStateException("OkxWebSocketProperties 未配置");
        }

        String url = websocketProperties.getUrl();
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalStateException("WebSocket URL 未配置");
        }

        // v1.0 只做 K 线（Public 频道），确保 URL 正确
        if (!url.contains("/ws/v5/public") && !url.contains("/ws/v5/business")) {
            log.warn("WebSocket URL 可能不正确，v1.0 应使用 /ws/v5/public 或 /ws/v5/business: {}", url);
        }

        log.info("配置验证通过: url={}, connectTimeout={}s", 
                url, websocketProperties.getConnectTimeoutSeconds());
    }

    /**
     * 构建 OkHttpClient
     */
    private void buildHttpClient() {
        Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", 7890));
        OkHttpClient.Builder builder = new OkHttpClient.Builder().proxy(proxy)
                .connectTimeout(websocketProperties.getConnectTimeoutSeconds(), TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS) // WebSocket 保持长连接，不设置读取超时
                .writeTimeout(0, TimeUnit.SECONDS)
                .pingInterval(20, TimeUnit.SECONDS); // OkHttp 内置 ping，每20秒一次

        this.httpClient = builder.build();
    }

    /**
     * 建立连接（异步，不阻塞）
     * 
     * <p>连接失败不会抛出异常，而是触发异步重连机制。
     * 这样可以避免启动时连接失败导致整个应用启动失败。
     */
    public void connect() {
        if (shutdown.get()) {
            log.warn("应用已关闭，跳过连接");
            return;
        }

        if (connected.get()) {
            log.warn("WebSocket 已连接，无需重复连接");
            return;
        }

        try {
            String url = websocketProperties.getUrl();
            log.info("尝试连接 WebSocket: {} (重连次数: {})", url, reconnectAttempts.get());

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            webSocket = httpClient.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(okhttp3.WebSocket webSocket, Response response) {
                    connected.set(true);
                    reconnectAttempts.set(0); // 连接成功，重置重连次数
                    lastMessageTime.set(System.currentTimeMillis()); // 更新最后消息时间
                    log.info("WebSocket 连接已建立: {}", url);
                    
                    // 启动心跳和静默检测
                    startHeartbeat();
                    startSilenceDetection();
                    
                    // 重连成功后，恢复所有期望的订阅
                    if (!desiredSubscriptions.isEmpty()) {
                        log.info("重连成功，恢复 {} 个订阅", desiredSubscriptions.size());
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
                    log.warn("WebSocket 正在关闭: code={}, reason={}", code, reason);
                }

                @Override
                public void onClosed(okhttp3.WebSocket webSocket, int code, String reason) {
                    connected.set(false);
                    stopHeartbeat();
                    stopSilenceDetection();
                    log.warn("WebSocket 连接关闭: code={}, reason={}", code, reason);
                    
                    // 如果应用未关闭，触发重连
                    if (!shutdown.get()) {
                        scheduleReconnect();
                    }
                }

                @Override
                public void onFailure(okhttp3.WebSocket webSocket, Throwable t, Response response) {
                    connected.set(false);
                    stopHeartbeat();
                    stopSilenceDetection();
                    log.error("WebSocket 连接失败: {}", t.getMessage(), t);
                    
                    // 如果应用未关闭，触发重连
                    if (!shutdown.get()) {
                        scheduleReconnect();
                    }
                }
            });

            // 异步等待连接建立（不阻塞启动流程）
            // 如果连接超时，会在 onFailure 回调中触发重连
            reconnectExecutor.execute(() -> {
                try {
                    long startTime = System.currentTimeMillis();
                    long timeout = websocketProperties.getConnectTimeoutSeconds() * 1000L;
                    
                    // 等待连接建立（最多等待超时时间）
                    while (!connected.get() && !shutdown.get() && (System.currentTimeMillis() - startTime) < timeout) {
                        Thread.sleep(100);
                    }

                    if (!connected.get() && !shutdown.get()) {
                        log.warn("WebSocket 连接超时，将在后台重连");
                        // 不抛出异常，而是触发重连机制
                        reconnectAttempts.incrementAndGet();
                        scheduleReconnect();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("等待连接建立被中断");
                } catch (Exception e) {
                    log.error("等待连接建立时发生异常", e);
                }
            });

        } catch (Exception e) {
            log.error("WebSocket 连接失败: {}", e.getMessage(), e);
            connected.set(false);
            reconnectAttempts.incrementAndGet();
            // 不抛出异常，而是触发重连机制
            if (!shutdown.get()) {
                scheduleReconnect();
            }
        }
    }

    /**
     * 处理消息
     */
    private void handleMessage(String message) {
        // 更新最后消息时间
        lastMessageTime.set(System.currentTimeMillis());

        // 处理 pong 响应（心跳响应）
        if ("pong".equals(message)) {
            log.debug("收到 pong 响应，连接正常");
            return;
        }

        // 路由消息
        channelRouter.route(message);
    }

    /**
     * 启动心跳
     */
    private void startHeartbeat() {
        stopHeartbeat(); // 先停止可能存在的旧任务

        if (heartbeatExecutor == null) {
            heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ExchangeWebSocket-Heartbeat");
                t.setDaemon(true);
                return t;
            });
        }

        heartbeatTask = heartbeatExecutor.scheduleAtFixedRate(() -> {
            try {
                if (connected.get() && webSocket != null) {
                    webSocket.send("ping");
                    log.debug("发送 ping 心跳");
                } else {
                    log.debug("连接未建立，跳过心跳");
                }
            } catch (Exception e) {
                log.warn("发送 ping 心跳失败: {}", e.getMessage());
            }
        }, HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);

        log.info("心跳任务已启动，间隔 {} 秒", HEARTBEAT_INTERVAL_SECONDS);
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
        stopSilenceDetection(); // 先停止可能存在的旧任务

        if (silenceDetectorExecutor == null) {
            silenceDetectorExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ExchangeWebSocket-SilenceDetector");
                t.setDaemon(true);
                return t;
            });
        }

        silenceDetectorTask = silenceDetectorExecutor.scheduleAtFixedRate(() -> {
            try {
                if (!connected.get()) {
                    return;
                }

                long lastTime = lastMessageTime.get();
                long now = System.currentTimeMillis();
                long silenceDuration = now - lastTime;

                if (silenceDuration > SILENCE_THRESHOLD_MS) {
                    log.warn("检测到静默连接（{} 秒无消息），触发重连", silenceDuration / 1000);
                    // 关闭当前连接，触发重连
                    if (webSocket != null) {
                        webSocket.close(1000, "Silence detected");
                    }
                }
            } catch (Exception e) {
                log.error("静默检测异常", e);
            }
        }, SILENCE_DETECTION_INTERVAL_SECONDS, SILENCE_DETECTION_INTERVAL_SECONDS, TimeUnit.SECONDS);

        log.info("静默检测任务已启动，间隔 {} 秒，阈值 {} 秒", 
                SILENCE_DETECTION_INTERVAL_SECONDS, SILENCE_THRESHOLD_MS / 1000);
    }

    /**
     * 停止静默检测
     */
    private void stopSilenceDetection() {
        if (silenceDetectorTask != null) {
            silenceDetectorTask.cancel(false);
            silenceDetectorTask = null;
        }
    }

    /**
     * 安排重连
     */
    private void scheduleReconnect() {
        if (shutdown.get()) {
            log.debug("应用已关闭，跳过重连");
            return;
        }

        if (reconnecting) {
            log.debug("重连已在进行中，跳过");
            return;
        }

        int attempts = reconnectAttempts.get();
        if (attempts >= MAX_RECONNECT_ATTEMPTS) {
            log.error("WebSocket 重连次数已达上限（{} 次），停止重连。请检查网络连接。", MAX_RECONNECT_ATTEMPTS);
            return;
        }

        reconnecting = true;

        // 计算延迟时间（指数退避）
        long delay = attempts < RECONNECT_DELAYS_MS.length 
                ? RECONNECT_DELAYS_MS[attempts] 
                : RECONNECT_DELAYS_MS[RECONNECT_DELAYS_MS.length - 1];

        reconnectExecutor.execute(() -> {
            try {
                log.info("等待 {} 秒后开始重连 WebSocket (尝试 {}/{})...", 
                        delay / 1000, attempts + 1, MAX_RECONNECT_ATTEMPTS);
                Thread.sleep(delay);

                // 再次检查是否已关闭
                if (shutdown.get()) {
                    log.debug("应用已关闭，取消重连");
                    reconnecting = false;
                    return;
                }

                reconnect();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("重连等待被中断");
                reconnecting = false;
            } catch (Exception e) {
                log.error("安排重连时发生异常", e);
                reconnecting = false;
            }
        });
    }

    /**
     * 重连
     */
    private void reconnect() {
        if (shutdown.get()) {
            log.debug("应用已关闭，取消重连");
            reconnecting = false;
            return;
        }

        try {
            // 关闭旧连接
            if (webSocket != null) {
                try {
                    webSocket.close(1000, "Reconnecting");
                } catch (Exception e) {
                    log.warn("关闭旧连接失败", e);
                }
                webSocket = null;
            }

            // 重新连接
            connect();

        } catch (Exception e) {
            log.error("WebSocket 重连失败: {}", e.getMessage(), e);
            reconnectAttempts.incrementAndGet();
            reconnecting = false;

            // 如果未达到最大重连次数，继续尝试重连
            if (reconnectAttempts.get() < MAX_RECONNECT_ATTEMPTS) {
                scheduleReconnect();
            } else {
                log.error("WebSocket 重连次数已达上限，停止重连");
            }
        }
    }

    /**
     * 订阅交易对
     * 
     * <p>更新 desiredSubscriptions，然后异步发送订阅指令。
     * 
     * @param symbols 交易对符号集合（如：["BTC-USDT-SWAP", "ETH-USDT-SWAP"]）
     */
    public void subscribe(Set<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            log.warn("订阅符号集合为空");
            return;
        }

        // 更新状态（Source of Truth）
        desiredSubscriptions.addAll(symbols);
        log.info("更新期望订阅列表，当前订阅数: {}", desiredSubscriptions.size());

        // 如果已连接，发送订阅指令
        if (connected.get() && webSocket != null) {
            sendSubscribeMessage(symbols);
        } else {
            log.debug("WebSocket 未连接，订阅将在连接建立后自动恢复");
        }
    }

    /**
     * 取消订阅交易对
     * 
     * <p>更新 desiredSubscriptions，然后异步发送取消订阅指令。
     * 
     * @param symbols 交易对符号集合
     */
    public void unsubscribe(Set<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            log.warn("取消订阅符号集合为空");
            return;
        }

        // 更新状态（Source of Truth）
        desiredSubscriptions.removeAll(symbols);
        log.info("更新期望订阅列表，当前订阅数: {}", desiredSubscriptions.size());

        // 如果已连接，发送取消订阅指令
        if (connected.get() && webSocket != null) {
            sendUnsubscribeMessage(symbols);
        }
    }

    /**
     * 订阅所有已注册的 Channel
     * 
     * <p>遍历所有已注册的 Channel，发送订阅消息。
     * 重连时会自动调用此方法恢复订阅。
     */
    public void subscribeAll() {
        if (!connected.get() || webSocket == null) {
            log.warn("WebSocket 未连接，无法订阅");
            return;
        }

        if (desiredSubscriptions.isEmpty()) {
            log.info("期望订阅列表为空，无需订阅");
            return;
        }

        // 发送订阅消息（按 Channel 分组）
        // v1.0 只支持 KLINE，直接发送
        sendSubscribeMessage(desiredSubscriptions);
    }

    /**
     * 发送订阅消息
     */
    private void sendSubscribeMessage(Set<String> symbols) {
        // 获取 KLINE Channel（v1.0 只支持 KLINE）
        // 这里简化处理，直接构建订阅消息
        // 实际应该从 ChannelRouter 获取对应的 Channel
        
        // 构建订阅消息
        StringBuilder args = new StringBuilder();
        for (String symbol : symbols) {
            if (args.length() > 0) {
                args.append(",");
            }
            args.append(String.format("{\"channel\":\"candle1m\",\"instId\":\"%s\"}", symbol));
        }

        String message = String.format("{\"op\":\"subscribe\",\"args\":[%s]}", args.toString());
        
        try {
            if (webSocket != null) {
                boolean sent = webSocket.send(message);
                if (sent) {
                    log.info("发送订阅消息成功: symbols={}", symbols);
                } else {
                    log.error("发送订阅消息失败：WebSocket 可能已关闭");
                }
            }
        } catch (Exception e) {
            log.error("发送订阅消息异常: symbols={}", symbols, e);
        }
    }

    /**
     * 发送取消订阅消息
     */
    private void sendUnsubscribeMessage(Set<String> symbols) {
        // 构建取消订阅消息
        StringBuilder args = new StringBuilder();
        for (String symbol : symbols) {
            if (args.length() > 0) {
                args.append(",");
            }
            args.append(String.format("{\"channel\":\"candle1m\",\"instId\":\"%s\"}", symbol));
        }

        String message = String.format("{\"op\":\"unsubscribe\",\"args\":[%s]}", args.toString());
        
        try {
            if (webSocket != null) {
                boolean sent = webSocket.send(message);
                if (sent) {
                    log.info("发送取消订阅消息成功: symbols={}", symbols);
                } else {
                    log.error("发送取消订阅消息失败：WebSocket 可能已关闭");
                }
            }
        } catch (Exception e) {
            log.error("发送取消订阅消息异常: symbols={}", symbols, e);
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
     * 
     * <p>只在应用关闭时调用（通过 @PreDestroy 注解）。
     * 启动过程中的超时或异常不会触发此方法。
     */
    @PreDestroy
    public void shutdown() {
        log.info("开始关闭 ExchangeWebSocketManager...");

        // 设置关闭标志位（阻止新的连接和重连）
        shutdown.set(true);

        // 停止心跳和静默检测
        stopHeartbeat();
        stopSilenceDetection();

        // 关闭 WebSocket 连接
        if (webSocket != null) {
            try {
                webSocket.close(1000, "Server shutdown");
            } catch (Exception e) {
                log.error("关闭 WebSocket 连接失败", e);
            }
            webSocket = null;
        }

        // 关闭线程池
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdown();
            try {
                if (!heartbeatExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
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
                if (!silenceDetectorExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    silenceDetectorExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                silenceDetectorExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        reconnectExecutor.shutdown();
        try {
            if (!reconnectExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                reconnectExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            reconnectExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        connected.set(false);
        log.info("ExchangeWebSocketManager 已关闭");
    }
}

