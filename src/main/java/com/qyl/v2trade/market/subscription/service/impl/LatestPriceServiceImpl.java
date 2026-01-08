package com.qyl.v2trade.market.subscription.service.impl;

import com.qyl.v2trade.market.model.event.PriceChangedEvent;
import com.qyl.v2trade.market.model.event.PriceTick;
import com.qyl.v2trade.market.subscription.collector.eventbus.PriceEventBus;
import com.qyl.v2trade.market.subscription.service.LatestPrice;
import com.qyl.v2trade.market.subscription.service.LatestPriceService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 最新价格服务实现
 * 
 * <p>维护每个交易对的最新价格状态（仅内存）。
 *
 * @author qyl
 */
@Slf4j
@Service
public class LatestPriceServiceImpl implements LatestPriceService {

    /**
     * 最新价格缓存（线程安全）
     * Key: 交易对符号（如：BTC-USDT-SWAP）
     * Value: LatestPrice实例
     */
    private final ConcurrentHashMap<String, LatestPrice> priceCache = new ConcurrentHashMap<>();

    /**
     * Spring事件发布器（用于发布PriceChangedEvent）
     */
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 价格事件总线（用于订阅PriceTick事件）
     */
    private final PriceEventBus priceEventBus;

    /**
     * PriceTick事件消费者（用于取消订阅）
     */
    private Consumer<PriceTick> priceTickConsumer;

    public LatestPriceServiceImpl(ApplicationEventPublisher eventPublisher, PriceEventBus priceEventBus) {
        this.eventPublisher = eventPublisher;
        this.priceEventBus = priceEventBus;
    }

    /**
     * 初始化：订阅PriceTick事件
     */
    @PostConstruct
    public void init() {
        priceTickConsumer = this::updatePrice;
        priceEventBus.subscribe(priceTickConsumer);
        log.info("LatestPriceService已订阅PriceTick事件");
    }

    /**
     * 清理：取消订阅PriceTick事件
     */
    @PreDestroy
    public void cleanup() {
        if (priceTickConsumer != null) {
            priceEventBus.unsubscribe(priceTickConsumer);
            log.info("LatestPriceService已取消订阅PriceTick事件");
        }
    }

    @Override
    public void updatePrice(PriceTick tick) {
        if (tick == null) {
            log.warn("PriceTick为null，跳过更新");
            return;
        }

        String symbol = tick.symbol();
        LatestPrice current = priceCache.get(symbol);

        // 时间戳过滤：旧消息直接丢弃
        if (current != null && tick.timestamp() < current.timestamp()) {
            log.debug("丢弃旧价格: symbol={}, oldTimestamp={}, newTimestamp={}", 
                    symbol, current.timestamp(), tick.timestamp());
            return;
        }

        // 更新最新价格（后到覆盖前到）
        LatestPrice latest = LatestPrice.of(symbol, tick.price(), tick.timestamp());
        priceCache.put(symbol, latest);

        // 发布PriceChangedEvent（异步，不阻塞）
        PriceChangedEvent event = PriceChangedEvent.of(symbol, tick.price(), tick.timestamp());
        eventPublisher.publishEvent(event);

    }

    @Override
    public Optional<LatestPrice> getLatestPrice(String symbol) {
        if (symbol == null) {
            return Optional.empty();
        }

        LatestPrice latest = priceCache.get(symbol);
        return Optional.ofNullable(latest);
    }

    @Override
    public BigDecimal getPrice(String symbol) {
        if (symbol == null) {
            return null;
        }

        LatestPrice latest = priceCache.get(symbol);
        return latest != null ? latest.price() : null;
    }

    @Override
    public Map<String, LatestPrice> getAllLatestPrices() {
        // 防御性拷贝，防止外部修改
        return Collections.unmodifiableMap(new ConcurrentHashMap<>(priceCache));
    }

    @Override
    public void removePrice(String symbol) {
        if (symbol == null) {
            return;
        }

        LatestPrice removed = priceCache.remove(symbol);
        if (removed != null) {
            log.debug("已清除价格: symbol={}", symbol);
        }
    }
}

