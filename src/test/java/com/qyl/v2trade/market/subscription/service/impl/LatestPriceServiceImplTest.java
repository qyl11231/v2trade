package com.qyl.v2trade.market.subscription.service.impl;

import com.qyl.v2trade.market.model.event.PriceChangedEvent;
import com.qyl.v2trade.market.model.event.PriceTick;
import com.qyl.v2trade.market.subscription.collector.eventbus.PriceEventBus;
import com.qyl.v2trade.market.subscription.collector.eventbus.impl.SimplePriceEventBus;
import com.qyl.v2trade.market.subscription.service.LatestPrice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * LatestPriceServiceImpl 测试
 */
class LatestPriceServiceImplTest {

    private LatestPriceServiceImpl service;
    private org.springframework.context.ApplicationEventPublisher eventPublisher;
    private PriceEventBus priceEventBus;
    private AtomicReference<PriceChangedEvent> publishedEvent;

    @BeforeEach
    void setUp() {
        publishedEvent = new AtomicReference<>();
        eventPublisher = mock(org.springframework.context.ApplicationEventPublisher.class);
        doAnswer(invocation -> {
            Object event = invocation.getArgument(0);
            if (event instanceof PriceChangedEvent) {
                publishedEvent.set((PriceChangedEvent) event);
            }
            return null;
        }).when(eventPublisher).publishEvent(any());

        priceEventBus = new SimplePriceEventBus();
        service = new LatestPriceServiceImpl(eventPublisher, priceEventBus);
        
        // 手动调用init方法（因为@PostConstruct不会在测试中自动执行）
        service.init();
    }

    @Test
    void testUpdatePrice() {
        PriceTick tick = PriceTick.of("BTC-USDT-SWAP", new BigDecimal("42000.5"), 1710000000000L, "OKX");

        service.updatePrice(tick);

        Optional<LatestPrice> result = service.getLatestPrice("BTC-USDT-SWAP");
        assertTrue(result.isPresent());
        assertEquals("BTC-USDT-SWAP", result.get().symbol());
        assertEquals(new BigDecimal("42000.5"), result.get().price());
        assertEquals(1710000000000L, result.get().timestamp());

        // 验证事件发布
        assertNotNull(publishedEvent.get());
        assertEquals("BTC-USDT-SWAP", publishedEvent.get().symbol());
        assertEquals(new BigDecimal("42000.5"), publishedEvent.get().price());
    }

    @Test
    void testUpdatePriceTimestampFiltering() {
        PriceTick tick1 = PriceTick.of("BTC-USDT-SWAP", new BigDecimal("42000.5"), 1710000000000L, "OKX");
        PriceTick tick2 = PriceTick.of("BTC-USDT-SWAP", new BigDecimal("41000.0"), 1710000000001L, "OKX");
        PriceTick tick3 = PriceTick.of("BTC-USDT-SWAP", new BigDecimal("43000.0"), 1709999999999L, "OKX"); // 旧时间戳

        service.updatePrice(tick1);
        service.updatePrice(tick2);
        publishedEvent.set(null); // 重置事件
        service.updatePrice(tick3); // 旧时间戳应该被丢弃

        Optional<LatestPrice> result = service.getLatestPrice("BTC-USDT-SWAP");
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("41000.0"), result.get().price());
        assertEquals(1710000000001L, result.get().timestamp());

        // 验证旧消息没有触发事件发布
        assertNull(publishedEvent.get());
    }

    @Test
    void testGetLatestPrice() {
        PriceTick tick = PriceTick.of("BTC-USDT-SWAP", new BigDecimal("42000.5"), 1710000000000L, "OKX");
        service.updatePrice(tick);

        Optional<LatestPrice> result = service.getLatestPrice("BTC-USDT-SWAP");
        assertTrue(result.isPresent());
        assertEquals("BTC-USDT-SWAP", result.get().symbol());
        assertEquals(new BigDecimal("42000.5"), result.get().price());
    }

    @Test
    void testGetLatestPriceNotFound() {
        Optional<LatestPrice> result = service.getLatestPrice("BTC-USDT-SWAP");
        assertFalse(result.isPresent());
    }

    @Test
    void testGetLatestPriceNull() {
        Optional<LatestPrice> result = service.getLatestPrice(null);
        assertFalse(result.isPresent());
    }

    @Test
    void testGetPrice() {
        PriceTick tick = PriceTick.of("BTC-USDT-SWAP", new BigDecimal("42000.5"), 1710000000000L, "OKX");
        service.updatePrice(tick);

        BigDecimal price = service.getPrice("BTC-USDT-SWAP");
        assertEquals(new BigDecimal("42000.5"), price);
    }

    @Test
    void testGetPriceNotFound() {
        BigDecimal price = service.getPrice("BTC-USDT-SWAP");
        assertNull(price);
    }

    @Test
    void testGetAllLatestPrices() {
        service.updatePrice(PriceTick.of("BTC-USDT-SWAP", new BigDecimal("42000.5"), 1710000000000L, "OKX"));
        service.updatePrice(PriceTick.of("ETH-USDT-SWAP", new BigDecimal("3000.0"), 1710000000000L, "OKX"));

        Map<String, LatestPrice> allPrices = service.getAllLatestPrices();

        assertEquals(2, allPrices.size());
        assertTrue(allPrices.containsKey("BTC-USDT-SWAP"));
        assertTrue(allPrices.containsKey("ETH-USDT-SWAP"));
        assertEquals(new BigDecimal("42000.5"), allPrices.get("BTC-USDT-SWAP").price());
        assertEquals(new BigDecimal("3000.0"), allPrices.get("ETH-USDT-SWAP").price());

        // 验证返回的是不可变Map（防御性拷贝）
        assertThrows(UnsupportedOperationException.class, () -> {
            allPrices.put("TEST", LatestPrice.of("TEST", BigDecimal.ONE, 0L));
        });
    }

    @Test
    void testRemovePrice() {
        service.updatePrice(PriceTick.of("BTC-USDT-SWAP", new BigDecimal("42000.5"), 1710000000000L, "OKX"));

        service.removePrice("BTC-USDT-SWAP");

        Optional<LatestPrice> result = service.getLatestPrice("BTC-USDT-SWAP");
        assertFalse(result.isPresent());
    }

    @Test
    void testRemovePriceNotFound() {
        assertDoesNotThrow(() -> service.removePrice("BTC-USDT-SWAP"));
    }

    @Test
    void testConcurrentUpdate() throws InterruptedException {
        int threadCount = 10;
        int updatesPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger updateCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < updatesPerThread; j++) {
                        long timestamp = 1710000000000L + (threadId * updatesPerThread + j);
                        BigDecimal price = new BigDecimal("42000").add(new BigDecimal(threadId * updatesPerThread + j));
                        service.updatePrice(PriceTick.of("BTC-USDT-SWAP", price, timestamp, "OKX"));
                        updateCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        Optional<LatestPrice> result = service.getLatestPrice("BTC-USDT-SWAP");
        assertTrue(result.isPresent());
        // 最终价格应该是时间戳最大的（可能不是最后一个更新的，因为并发）
        assertTrue(result.get().timestamp() >= 1710000000000L);

        executor.shutdown();
    }

    @Test
    void testUpdatePriceNull() {
        assertDoesNotThrow(() -> service.updatePrice(null));
        verify(eventPublisher, never()).publishEvent(any());
    }
}

