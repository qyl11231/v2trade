package com.qyl.v2trade.market.subscription.processor;

import com.qyl.v2trade.business.system.model.entity.ExchangeMarketPair;
import com.qyl.v2trade.business.system.model.entity.MarketSubscriptionConfig;
import com.qyl.v2trade.business.system.model.entity.TradingPair;
import com.qyl.v2trade.business.system.service.ExchangeMarketPairService;
import com.qyl.v2trade.business.system.service.MarketSubscriptionConfigService;
import com.qyl.v2trade.business.system.service.TradingPairService;
import com.qyl.v2trade.common.constants.ExchangeCode;
import com.qyl.v2trade.market.subscription.persistence.cache.MarketCacheService;
import com.qyl.v2trade.market.subscription.persistence.cache.impl.RedisMarketCacheService;
import com.qyl.v2trade.market.subscription.delivery.distributor.MarketDistributor;
import com.qyl.v2trade.market.subscription.collector.eventbus.MarketEventBus;
import com.qyl.v2trade.market.subscription.collector.ingestor.MarketIngestor;
import com.qyl.v2trade.market.subscription.collector.ingestor.impl.OkxMarketIngestor;
import com.qyl.v2trade.market.model.NormalizedKline;
import com.qyl.v2trade.market.model.event.KlineEvent;
import com.qyl.v2trade.market.subscription.infrastructure.monitor.MarketDataMonitor;
// import com.qyl.v2trade.market.web.query.MarketQueryService; // 暂时未使用
import com.qyl.v2trade.market.subscription.persistence.storage.MarketStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 行情数据中心主服务
 * 
 * <p>职责：实时行情接收器（单一职责）
 * <ul>
 *   <li>订阅 EventBus 接收 KlineEvent</li>
 *   <li>接收最新一根 K 线</li>
 *   <li>将 K 线写入行情数据通道（QuestDB / 内存分发）</li>
 * </ul>
 * 
 * <p>不负责：
 * <ul>
 *   <li>K 线连续性检测（由补拉模块负责）</li>
 *   <li>历史数据补拉（由补拉模块负责）</li>
 *   <li>REST API 回补（由补拉模块负责）</li>
 *   <li>启动时扫描时间戳 gap（由补拉模块负责）</li>
 * </ul>
 *
 * @author qyl
 */
@Slf4j
@Component
public class MarketDataCenter implements ApplicationRunner {

    @Autowired
    private MarketIngestor marketIngestor;

    @Autowired
    private MarketEventBus eventBus;

    @Autowired
    private MarketStorageService marketStorageService;

    @Autowired
    private MarketCacheService marketCacheService;

    @Autowired
    private MarketDistributor marketDistributor;

    @Autowired
    private MarketSubscriptionConfigService subscriptionConfigService;

    @Autowired
    private TradingPairService tradingPairService;

    @Autowired
    private ExchangeMarketPairService exchangeMarketPairService;

    // MarketQueryService 暂时未使用，保留以备将来使用
    // @Autowired
    // private MarketQueryService marketQueryService;

    @Autowired
    private MarketDataMonitor marketDataMonitor;

    private volatile boolean running = false;

    /**
     * 内存去重缓存（防止EventBus异步导致的并发插入）
     * Key: symbol:timestamp (毫秒级时间戳)
     * Value: 处理时间（用于清理过期缓存）
     */
    private final java.util.concurrent.ConcurrentHashMap<String, Long> deduplicationCache = 
            new java.util.concurrent.ConcurrentHashMap<>();
    
    /**
     * 缓存清理间隔（毫秒）
     */
    private static final long CACHE_CLEANUP_INTERVAL = 5 * 60 * 1000; // 5分钟
    
    /**
     * 缓存过期时间（毫秒）
     */
    private static final long CACHE_EXPIRE_TIME = 10 * 60 * 1000; // 10分钟
    
    /**
     * 上次清理时间
     */
    private volatile long lastCleanupTime = System.currentTimeMillis();

    @Override
    public void run(ApplicationArguments args) {
        log.info("行情数据中心启动中...");
        start();
    }

    /**
     * 启动行情数据中心
     */
    public void start() {
        if (running) {
            log.warn("行情数据中心已在运行");
            return;
        }

        try {
            // 1. 订阅 EventBus（接收 KlineEvent）
            eventBus.subscribe(this::handleKlineEvent);
            log.info("已订阅 EventBus");

            // 2. 启动行情采集（会启动 WebSocket 连接，异步不阻塞）
            log.info("启动行情采集服务...");
            marketIngestor.start();

            // 3. 等待 WebSocket 连接建立（最多等待10秒，但不阻塞应用启动）
            // 如果连接失败，允许应用启动，后台会自动重连
            boolean connected = waitForWebSocketConnection(10);
            if (!connected) {
                log.warn("WebSocket 连接未在启动时建立，将在后台自动重连");
                // 不抛出异常，允许应用启动
            }

            // 4. 加载并订阅所有启用的交易对
            // 即使连接未建立，也先加载配置，连接建立后会自动订阅
            log.info("开始加载并订阅交易对...");
            loadAndSubscribe();

            running = true;
            log.info("行情数据中心启动成功（WebSocket 连接状态: {}）", 
                    marketIngestor instanceof OkxMarketIngestor 
                            ? ((OkxMarketIngestor) marketIngestor).isWebSocketConnected() 
                            : "未知");
        } catch (Exception e) {
            log.error("行情数据中心启动失败", e);
            // 启动失败不应该导致整个应用无法启动
            // 改为记录错误，允许应用继续运行，后台会重连
            log.error("行情数据中心启动异常，但允许应用继续运行，将在后台重试", e);
            running = true; // 标记为运行中，允许后台重连
        }
    }

    /**
     * 等待 WebSocket 连接建立
     * 
     * @param timeoutSeconds 超时时间（秒）
     * @return true 表示连接已建立，false 表示超时
     */
    private boolean waitForWebSocketConnection(int timeoutSeconds) {
        if (marketIngestor instanceof OkxMarketIngestor) {
            OkxMarketIngestor okxIngestor = (OkxMarketIngestor) marketIngestor;
            long startTime = System.currentTimeMillis();
            while (!okxIngestor.isWebSocketConnected()) {
                if (System.currentTimeMillis() - startTime > timeoutSeconds * 1000L) {
                    log.warn("等待 WebSocket 连接超时（{} 秒），将在后台自动重连", timeoutSeconds);
                    return false; // 返回 false，不抛出异常
                }
                try {
                    Thread.sleep(500); // 每500ms检查一次
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("等待连接被中断");
                    return false;
                }
            }
            log.info("WebSocket 连接已建立");
            return true;
        }
        return false;
    }

    /**
     * 停止行情数据中心
     */
    public void stop() {
        if (!running) {
            return;
        }

        try {
            marketIngestor.stop();
            running = false;
            log.info("行情数据中心已停止");
        } catch (Exception e) {
            log.error("行情数据中心停止失败", e);
        }
    }

    /**
     * 加载并订阅所有启用的交易对
     * 
     * <p>可被外部调用（如重连时重新加载订阅）
     * 
     * <p>注意：此方法不检查 running 状态，因为：
     * <ul>
     *   <li>启动时调用：此时 running 还未设置为 true（在 start() 最后才设置）</li>
     *   <li>重连时调用：此时应用应该正常运行，running 应该是 true</li>
     *   <li>如果应用已关闭，shutdown 标志位已经可以阻止重连了</li>
     * </ul>
     */
    public void loadAndSubscribe() {
        try {
            // 获取所有启用的订阅配置
            List<MarketSubscriptionConfig> configs = subscriptionConfigService.listEnabled();
            log.info("加载行情订阅配置: count={}", configs.size());

            for (MarketSubscriptionConfig config : configs) {
                try {
                    // 获取交易对信息
                    TradingPair tradingPair = tradingPairService.getById(config.getTradingPairId());
                    if (tradingPair == null) {
                        log.warn("交易对不存在: tradingPairId={}", config.getTradingPairId());
                        continue;
                    }

                    // 获取交易所交易规则
                    ExchangeMarketPair exchangePair = exchangeMarketPairService
                            .getByExchangeAndTradingPairId(ExchangeCode.OKX, config.getTradingPairId());
                    if (exchangePair == null) {
                        log.warn("交易所交易规则不存在: tradingPairId={}", config.getTradingPairId());
                        continue;
                    }

                    // 订阅行情（不进行历史数据初始化，由补拉模块负责）
                    marketIngestor.subscribe(
                            tradingPair.getId(),
                            exchangePair.getSymbolOnExchange(),
                            tradingPair.getSymbol()
                    );

                    log.info("订阅行情成功: tradingPairId={}, symbol={}", 
                            tradingPair.getId(), tradingPair.getSymbol());
                } catch (Exception e) {
                    log.error("订阅行情失败: tradingPairId={}", config.getTradingPairId(), e);
                }
            }
        } catch (Exception e) {
            log.error("加载行情订阅配置失败", e);
        }
    }

    /**
     * 处理 KlineEvent（从 EventBus 接收）
     * 
     * <p>将 KlineEvent 转换为 NormalizedKline，然后调用原有的处理逻辑。
     * 
     * @param event KlineEvent
     */
    private void handleKlineEvent(KlineEvent event) {
        try {
            // 1. 内存去重（防止EventBus异步导致的并发插入）
            // 对齐时间戳到分钟（确保同一根K线的时间戳一致）
            long alignedTimestamp = alignTimestampToMinute(event.openTime());
            String dedupKey = event.symbol() + ":" + alignedTimestamp;
            
            // 检查是否已处理过
            Long processedTime = deduplicationCache.get(dedupKey);
            if (processedTime != null) {
                log.debug("K线事件已处理（内存去重）: symbol={}, timestamp={}, alignedTimestamp={}", 
                        event.symbol(), event.openTime(), alignedTimestamp);
                return;
            }
            
            // 2. 过滤掉开高低收价格相同的K线（数据异常）
            if (event.open().equals(event.high()) && 
                event.high().equals(event.low()) && 
                event.low().equals(event.close())) {
                log.warn("跳过开高低收价格相同的K线: symbol={}, timestamp={}, price={}", 
                        event.symbol(), event.openTime(), event.open());
                return;
            }

            // 3. 转换为 NormalizedKline（使用对齐后的时间戳）
            NormalizedKline kline = OkxMarketIngestor.convertToNormalizedKline(event);
            // 使用对齐后的时间戳（UTC epoch millis，分钟起始点）
            kline.setTimestamp(alignedTimestamp);
            
            // 4. 标记为已处理（在调用handleKline之前，防止并发）
            deduplicationCache.put(dedupKey, System.currentTimeMillis());
            
            // 5. 定期清理过期缓存
            cleanupDeduplicationCache();

            // 6. 调用原有的处理逻辑
            handleKline(kline);

        } catch (Exception e) {
            // 日志同时打印UTC和本地时间
            Instant eventTime = Instant.ofEpochMilli(event.openTime());
            ZonedDateTime utcTime = eventTime.atZone(ZoneId.of("UTC"));
            ZonedDateTime localTime = eventTime.atZone(ZoneId.of("Asia/Shanghai"));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            log.error("处理 KlineEvent 失败: symbol={}, timestamp={} (UTC: {}, CST: {})", 
                    event.symbol(), event.openTime(), 
                    utcTime.format(formatter), localTime.format(formatter), e);
        }
    }
    
    /**
     * 将时间戳对齐到分钟起始点（向下取整，UTC语义）
     * 
     * <p>K线时间戳必须对齐到周期起始点，例如1m K线的ts必须是整分钟的epoch millis。
     * 例如：171000001234 -> 171000000000 (UTC)
     * 
     * <p>时间语义：所有时间戳都是UTC epoch millis，对齐后仍然是UTC。
     * 
     * @param timestamp 原始时间戳（毫秒，UTC epoch millis）
     * @return 对齐后的时间戳（毫秒，UTC epoch millis，分钟起始点）
     */
    private long alignTimestampToMinute(long timestamp) {
        // 向下取整到分钟：去掉秒和毫秒部分
        return (timestamp / 60000) * 60000;
    }
    
    /**
     * 清理过期的去重缓存
     */
    private void cleanupDeduplicationCache() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupTime < CACHE_CLEANUP_INTERVAL) {
            return; // 未到清理时间
        }
        
        lastCleanupTime = now;
        long expireThreshold = now - CACHE_EXPIRE_TIME;
        
        int removedCount = 0;
        for (java.util.Iterator<java.util.Map.Entry<String, Long>> it = deduplicationCache.entrySet().iterator(); 
             it.hasNext();) {
            java.util.Map.Entry<String, Long> entry = it.next();
            if (entry.getValue() < expireThreshold) {
                it.remove();
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            log.debug("清理过期去重缓存: 移除{}条, 剩余{}条", removedCount, deduplicationCache.size());
        }
    }

    /**
     * 处理 K 线数据
     * 
     * <p>职责：只接收最新 K 线，写入 QuestDB 并分发
     * <ul>
     *   <li>不进行 gap 检测（由补拉模块负责）</li>
     *   <li>不进行历史数据补拉（由补拉模块负责）</li>
     *   <li>只检测上一根 K 线与当前 K 线是否一致，避免重复</li>
     * </ul>
     * 
     * @param kline NormalizedKline
     */
    private void handleKline(NormalizedKline kline) {
        try {
            // 日志同时打印UTC和本地时间
            Instant klineTime = Instant.ofEpochMilli(kline.getTimestamp());
            ZonedDateTime utcTime = klineTime.atZone(ZoneId.of("UTC"));
            ZonedDateTime localTime = klineTime.atZone(ZoneId.of("Asia/Shanghai"));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            log.debug("开始处理 K 线: symbol={}, timestamp={} (UTC: {}, CST: {}), close={}", 
                    kline.getSymbol(), kline.getTimestamp(), 
                    utcTime.format(formatter), localTime.format(formatter),
                    kline.getClose());

            // 记录收到 K 线
            marketDataMonitor.recordKlineReceived();

            // 1. 保存到 QuestDB（存储层会处理相同时间戳的去重，保证同一时间戳只保留一条数据）
            // 时间戳语义：UTC epoch millis，对齐到分钟起始点
            boolean saved = marketStorageService.saveKline(kline);
            if (saved) {
                marketDataMonitor.recordKlineSaved();
                log.debug("K 线已保存: symbol={}, timestamp={} (UTC: {}, CST: {})", 
                        kline.getSymbol(), kline.getTimestamp(),
                        utcTime.format(formatter), localTime.format(formatter));
            } else {
                log.debug("K 线已存在或保存失败: symbol={}, timestamp={} (UTC: {}, CST: {})", 
                        kline.getSymbol(), kline.getTimestamp(),
                        utcTime.format(formatter), localTime.format(formatter));
            }

            // 2. 检查 Redis 连接并缓存到 Redis（获取缓存时长配置）
            checkRedisConnection();
            MarketSubscriptionConfig config = findSubscriptionConfig(kline.getSymbol());
            int cacheDuration = config != null ? config.getCacheDurationMinutes() : 60;
            marketCacheService.cacheKline(kline, cacheDuration);

            // 3. 推送给所有订阅的 WebSocket 客户端
            marketDistributor.broadcastKline(kline);

            log.debug("K 线处理完成: symbol={}, timestamp={}", kline.getSymbol(), kline.getTimestamp());
        } catch (Exception e) {
            // 日志同时打印UTC和本地时间
            Instant klineTime = Instant.ofEpochMilli(kline.getTimestamp());
            ZonedDateTime utcTime = klineTime.atZone(ZoneId.of("UTC"));
            ZonedDateTime localTime = klineTime.atZone(ZoneId.of("Asia/Shanghai"));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            log.error("处理 K 线数据失败: symbol={}, timestamp={} (UTC: {}, CST: {})", 
                    kline.getSymbol(), kline.getTimestamp(),
                    utcTime.format(formatter), localTime.format(formatter), e);
        }
    }

    /**
     * 检查 Redis 连接状态并尝试恢复
     */
    private void checkRedisConnection() {
        if (marketCacheService instanceof RedisMarketCacheService) {
            RedisMarketCacheService redisCacheService = (RedisMarketCacheService) marketCacheService;
            redisCacheService.checkAndRecoverRedisConnection();
        }
    }

    /**
     * 查找订阅配置（根据交易对符号）
     * 
     * @param symbol 交易对符号
     * @return MarketSubscriptionConfig
     */
    private MarketSubscriptionConfig findSubscriptionConfig(String symbol) {
        try {
            // 根据 symbol 查找交易对
            TradingPair tradingPair = tradingPairService.getBySymbolAndMarketType(symbol, "SWAP");
            if (tradingPair == null) {
                return null;
            }

            return subscriptionConfigService.getByTradingPairId(tradingPair.getId());
        } catch (Exception e) {
            log.warn("查找订阅配置失败: symbol={}", symbol, e);
            return null;
        }
    }

    /**
     * 检查是否在运行
     * 
     * @return true 表示正在运行
     */
    public boolean isRunning() {
        return running;
    }
}
