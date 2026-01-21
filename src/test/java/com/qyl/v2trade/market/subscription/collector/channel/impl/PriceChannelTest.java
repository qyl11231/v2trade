package com.qyl.v2trade.market.subscription.collector.channel.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qyl.v2trade.market.model.event.PriceTick;
import com.qyl.v2trade.market.subscription.collector.channel.MarketChannel;
import com.qyl.v2trade.market.subscription.collector.eventbus.PriceEventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PriceChannel 测试
 */
class PriceChannelTest {

    private PriceChannel priceChannel;
    private ObjectMapper objectMapper;
    private AtomicReference<PriceTick> publishedTick;
    private PriceEventBus priceEventBus;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        publishedTick = new AtomicReference<>();
        priceEventBus = new PriceEventBus() {
            @Override
            public void publish(PriceTick tick) {
                publishedTick.set(tick);
            }

            @Override
            public void subscribe(java.util.function.Consumer<PriceTick> consumer) {
            }

            @Override
            public void unsubscribe(java.util.function.Consumer<PriceTick> consumer) {
            }
        };
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
    void testChannelType() {
        assertEquals(MarketChannel.CHANNEL_TYPE_PRICE, priceChannel.channelType());
    }

    @Test
    void testBuildSubscribeRaw() {
        Set<String> symbols = Set.of("BTC-USDT-SWAP", "ETH-USDT-SWAP");
        String result = priceChannel.buildSubscribeRaw(symbols);

        assertNotNull(result);
        assertTrue(result.contains("\"op\":\"subscribe\""));
        assertTrue(result.contains("\"channel\":\"ticker\""));
        assertTrue(result.contains("\"instId\":\"BTC-USDT-SWAP\""));
        assertTrue(result.contains("\"instId\":\"ETH-USDT-SWAP\""));
    }

    @Test
    void testBuildSubscribeRawEmpty() {
        String result = priceChannel.buildSubscribeRaw(Set.of());
        assertNull(result);
    }

    @Test
    void testBuildSubscribeRawNull() {
        String result = priceChannel.buildSubscribeRaw(null);
        assertNull(result);
    }

    @Test
    void testBuildUnsubscribeRaw() {
        Set<String> symbols = Set.of("BTC-USDT-SWAP", "ETH-USDT-SWAP");
        String result = priceChannel.buildUnsubscribeRaw(symbols);

        assertNotNull(result);
        assertTrue(result.contains("\"op\":\"unsubscribe\""));
        assertTrue(result.contains("\"channel\":\"ticker\""));
        assertTrue(result.contains("\"instId\":\"BTC-USDT-SWAP\""));
    }

    @Test
    void testOnMessage() {
        String tickerMessage = "{\"arg\":{\"channel\":\"ticker\",\"instId\":\"BTC-USDT-SWAP\"},\"data\":[{\"instId\":\"BTC-USDT-SWAP\",\"last\":\"42000.5\",\"ts\":\"1710000000000\"}]}";

        priceChannel.onMessage(tickerMessage);

        assertNotNull(publishedTick.get());
        assertEquals("BTC-USDT-SWAP", publishedTick.get().symbol());
    }

    @Test
    void testOnMessageWithInvalidJson() {
        String invalidMessage = "invalid json";
        publishedTick.set(null);

        assertDoesNotThrow(() -> priceChannel.onMessage(invalidMessage));
        assertNull(publishedTick.get());
    }

    @Test
    void testOnMessageWithEmptyData() {
        String message = "{\"arg\":{\"channel\":\"ticker\",\"instId\":\"BTC-USDT-SWAP\"},\"data\":[]}";
        publishedTick.set(null);

        priceChannel.onMessage(message);

        assertNull(publishedTick.get());
    }

    @Test
    void testOnMessageWithMissingLast() {
        String message = "{\"arg\":{\"channel\":\"ticker\",\"instId\":\"BTC-USDT-SWAP\"},\"data\":[{\"instId\":\"BTC-USDT-SWAP\",\"ts\":\"1710000000000\"}]}";
        publishedTick.set(null);

        priceChannel.onMessage(message);

        assertNull(publishedTick.get());
    }

    @Test
    void testOnMessageWithMissingTimestamp() {
        String message = "{\"arg\":{\"channel\":\"ticker\",\"instId\":\"BTC-USDT-SWAP\"},\"data\":[{\"instId\":\"BTC-USDT-SWAP\",\"last\":\"42000.5\"}]}";
        publishedTick.set(null);

        priceChannel.onMessage(message);

        // 应该使用当前时间（会有警告日志）
        assertNotNull(publishedTick.get());
        assertEquals("BTC-USDT-SWAP", publishedTick.get().symbol());
        assertEquals(new BigDecimal("42000.5"), publishedTick.get().price());
    }

    @Test
    void testOnMessageParsesCorrectly() {
        String tickerMessage = "{\"arg\":{\"channel\":\"ticker\",\"instId\":\"BTC-USDT-SWAP\"},\"data\":[{\"instId\":\"BTC-USDT-SWAP\",\"last\":\"42000.5\",\"ts\":\"1710000000000\"}]}";
        publishedTick.set(null);

        priceChannel.onMessage(tickerMessage);

        assertNotNull(publishedTick.get());
        assertEquals("BTC-USDT-SWAP", publishedTick.get().symbol());
        assertEquals(new BigDecimal("42000.5"), publishedTick.get().price());
        assertEquals(1710000000000L, publishedTick.get().timestamp());
        assertEquals("OKX", publishedTick.get().source());
    }
}

