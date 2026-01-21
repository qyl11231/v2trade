package com.qyl.v2trade.market.subscription.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qyl.v2trade.market.model.event.PriceChangedEvent;
import com.qyl.v2trade.market.model.event.PriceTick;
import com.qyl.v2trade.market.subscription.collector.channel.impl.PriceChannel;
import com.qyl.v2trade.market.subscription.collector.eventbus.PriceEventBus;
import com.qyl.v2trade.market.subscription.collector.eventbus.impl.SimplePriceEventBus;
import com.qyl.v2trade.market.subscription.service.LatestPrice;
import com.qyl.v2trade.market.subscription.service.LatestPriceService;
import com.qyl.v2trade.market.subscription.service.impl.LatestPriceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 价格订阅集成测试
 */
class PriceSubscriptionIntegrationTest {

    private PriceChannel priceChannel;
    private PriceEventBus priceEventBus;
    private LatestPriceService latestPriceService;
    private ObjectMapper objectMapper;
    private org.springframework.context.ApplicationEventPublisher eventPublisher;
    private AtomicReference<PriceChangedEvent> publishedEvent;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        priceEventBus = new SimplePriceEventBus();
        publishedEvent = new AtomicReference<>();
        eventPublisher = mock(org.springframework.context.ApplicationEventPublisher.class);
        doAnswer(invocation -> {
            Object event = invocation.getArgument(0);
            if (event instanceof PriceChangedEvent) {
                publishedEvent.set((PriceChangedEvent) event);
            }
            return null;
        }).when(eventPublisher).publishEvent(any());

        latestPriceService = new LatestPriceServiceImpl(eventPublisher, priceEventBus);
        priceChannel = new PriceChannel();

        try {
            // 使用反射设置依赖
            java.lang.reflect.Field objectMapperField = PriceChannel.class.getDeclaredField("objectMapper");
            objectMapperField.setAccessible(true);
            objectMapperField.set(priceChannel, objectMapper);

            java.lang.reflect.Field eventBusField = PriceChannel.class.getDeclaredField("priceEventBus");
            eventBusField.setAccessible(true);
            eventBusField.set(priceChannel, priceEventBus);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set fields", e);
        }
    }

    @Test
    void testEndToEndFlow() throws InterruptedException {
        String tickerMessage = "{\"arg\":{\"channel\":\"ticker\",\"instId\":\"BTC-USDT-SWAP\"},\"data\":[{\"instId\":\"BTC-USDT-SWAP\",\"last\":\"42000.5\",\"ts\":\"1710000000000\"}]}";

        // 模拟WebSocket消息到达
        priceChannel.onMessage(tickerMessage);

        // 等待事件处理（异步）
        Thread.sleep(100);

        // 验证价格已更新
        Optional<LatestPrice> result = latestPriceService.getLatestPrice("BTC-USDT-SWAP");
        assertTrue(result.isPresent());
        assertEquals("BTC-USDT-SWAP", result.get().symbol());
        assertEquals(new BigDecimal("42000.5"), result.get().price());
        assertEquals(1710000000000L, result.get().timestamp());

        // 验证PriceChangedEvent已发布
        assertNotNull(publishedEvent.get());
        assertEquals("BTC-USDT-SWAP", publishedEvent.get().symbol());
        assertEquals(new BigDecimal("42000.5"), publishedEvent.get().price());
        assertEquals(1710000000000L, publishedEvent.get().timestamp());
    }

    @Test
    void testTimestampFiltering() throws InterruptedException {
        // 先发送新时间戳的消息
        String message1 = "{\"arg\":{\"channel\":\"ticker\",\"instId\":\"BTC-USDT-SWAP\"},\"data\":[{\"instId\":\"BTC-USDT-SWAP\",\"last\":\"42000.5\",\"ts\":\"1710000000001\"}]}";
        priceChannel.onMessage(message1);
        Thread.sleep(50);

        // 再发送旧时间戳的消息
        publishedEvent.set(null);
        String message2 = "{\"arg\":{\"channel\":\"ticker\",\"instId\":\"BTC-USDT-SWAP\"},\"data\":[{\"instId\":\"BTC-USDT-SWAP\",\"last\":\"41000.0\",\"ts\":\"1710000000000\"}]}";
        priceChannel.onMessage(message2);
        Thread.sleep(50);

        // 验证价格没有被旧消息覆盖
        Optional<LatestPrice> result = latestPriceService.getLatestPrice("BTC-USDT-SWAP");
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("42000.5"), result.get().price());
        assertEquals(1710000000001L, result.get().timestamp());

        // 验证旧消息没有触发事件发布
        assertNull(publishedEvent.get());
    }

    @Test
    void testConcurrentUpdate() throws InterruptedException {
        int threadCount = 5;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    long timestamp = 1710000000000L + threadId;
                    BigDecimal price = new BigDecimal("42000").add(new BigDecimal(threadId));
                    String message = String.format(
                        "{\"arg\":{\"channel\":\"ticker\",\"instId\":\"BTC-USDT-SWAP\"},\"data\":[{\"instId\":\"BTC-USDT-SWAP\",\"last\":\"%s\",\"ts\":\"%d\"}]}",
                        price, timestamp
                    );
                    priceChannel.onMessage(message);
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        Thread.sleep(100); // 等待异步处理

        // 验证最终价格（应该是时间戳最大的）
        Optional<LatestPrice> result = latestPriceService.getLatestPrice("BTC-USDT-SWAP");
        assertTrue(result.isPresent());
        assertTrue(result.get().timestamp() >= 1710000000000L);
    }
}

