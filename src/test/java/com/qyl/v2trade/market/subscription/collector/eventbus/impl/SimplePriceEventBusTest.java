package com.qyl.v2trade.market.subscription.collector.eventbus.impl;

import com.qyl.v2trade.market.model.event.PriceTick;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SimplePriceEventBus 测试
 */
class SimplePriceEventBusTest {

    private SimplePriceEventBus eventBus;

    @BeforeEach
    void setUp() {
        eventBus = new SimplePriceEventBus();
    }

    @Test
    void testPublishAndSubscribe() throws InterruptedException {
        AtomicInteger receivedCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        PriceTick tick = PriceTick.of("BTC-USDT-SWAP", new BigDecimal("42000.5"), 1710000000000L, "OKX");

        eventBus.subscribe(t -> {
            receivedCount.incrementAndGet();
            assertEquals("BTC-USDT-SWAP", t.symbol());
            assertEquals(new BigDecimal("42000.5"), t.price());
            latch.countDown();
        });

        eventBus.publish(tick);

        assertTrue(latch.await(1, TimeUnit.SECONDS), "事件应该被接收");
        assertEquals(1, receivedCount.get());
    }

    @Test
    void testMultipleSubscribers() throws InterruptedException {
        AtomicInteger receivedCount1 = new AtomicInteger(0);
        AtomicInteger receivedCount2 = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);

        PriceTick tick = PriceTick.of("BTC-USDT-SWAP", new BigDecimal("42000.5"), 1710000000000L, "OKX");

        eventBus.subscribe(t -> {
            receivedCount1.incrementAndGet();
            latch.countDown();
        });

        eventBus.subscribe(t -> {
            receivedCount2.incrementAndGet();
            latch.countDown();
        });

        eventBus.publish(tick);

        assertTrue(latch.await(1, TimeUnit.SECONDS), "所有订阅者都应该接收事件");
        assertEquals(1, receivedCount1.get());
        assertEquals(1, receivedCount2.get());
    }

    @Test
    void testExceptionIsolation() throws InterruptedException {
        AtomicInteger receivedCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);

        PriceTick tick = PriceTick.of("BTC-USDT-SWAP", new BigDecimal("42000.5"), 1710000000000L, "OKX");

        // 第一个订阅者抛出异常
        eventBus.subscribe(t -> {
            throw new RuntimeException("测试异常");
        });

        // 第二个订阅者正常处理
        eventBus.subscribe(t -> {
            receivedCount.incrementAndGet();
            latch.countDown();
        });

        // 再发布一次，确保第一个订阅者异常不影响第二个
        eventBus.subscribe(t -> {
            receivedCount.incrementAndGet();
            latch.countDown();
        });

        eventBus.publish(tick);

        assertTrue(latch.await(1, TimeUnit.SECONDS), "异常订阅者不应该影响其他订阅者");
        assertEquals(2, receivedCount.get(), "正常订阅者应该接收到事件");
    }

    @Test
    void testUnsubscribe() throws InterruptedException {
        AtomicInteger receivedCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        PriceTick tick = PriceTick.of("BTC-USDT-SWAP", new BigDecimal("42000.5"), 1710000000000L, "OKX");

        var consumer = new java.util.function.Consumer<PriceTick>() {
            @Override
            public void accept(PriceTick t) {
                receivedCount.incrementAndGet();
                latch.countDown();
            }
        };

        eventBus.subscribe(consumer);
        eventBus.publish(tick);
        assertTrue(latch.await(1, TimeUnit.SECONDS), "第一次发布应该被接收");
        assertEquals(1, receivedCount.get());

        // 取消订阅
        eventBus.unsubscribe(consumer);
        CountDownLatch latch2 = new CountDownLatch(1);
        eventBus.publish(tick);
        assertFalse(latch2.await(100, TimeUnit.MILLISECONDS), "取消订阅后不应该接收事件");
        assertEquals(1, receivedCount.get(), "计数不应该增加");
    }

    @Test
    void testPublishNull() {
        // 发布null应该不会抛出异常
        assertDoesNotThrow(() -> eventBus.publish(null));
    }

    @Test
    void testNoSubscribers() {
        PriceTick tick = PriceTick.of("BTC-USDT-SWAP", new BigDecimal("42000.5"), 1710000000000L, "OKX");
        // 没有订阅者时发布应该不会抛出异常
        assertDoesNotThrow(() -> eventBus.publish(tick));
    }
}

