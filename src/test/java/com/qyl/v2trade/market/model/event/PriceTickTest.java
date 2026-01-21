package com.qyl.v2trade.market.model.event;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PriceTick 事件模型测试
 */
class PriceTickTest {

    @Test
    void testOf() {
        String symbol = "BTC-USDT-SWAP";
        BigDecimal price = new BigDecimal("42000.5");
        long timestamp = 1710000000000L;
        String source = "OKX";

        PriceTick tick = PriceTick.of(symbol, price, timestamp, source);

        assertNotNull(tick);
        assertEquals(symbol, tick.symbol());
        assertEquals(price, tick.price());
        assertEquals(timestamp, tick.timestamp());
        assertEquals(source, tick.source());
    }

    @Test
    void testEquals() {
        String symbol = "BTC-USDT-SWAP";
        BigDecimal price = new BigDecimal("42000.5");
        long timestamp = 1710000000000L;
        String source = "OKX";

        PriceTick tick1 = PriceTick.of(symbol, price, timestamp, source);
        PriceTick tick2 = PriceTick.of(symbol, price, timestamp, source);

        assertEquals(tick1, tick2);
        assertEquals(tick1.hashCode(), tick2.hashCode());
    }

    @Test
    void testNotEquals() {
        PriceTick tick1 = PriceTick.of("BTC-USDT-SWAP", new BigDecimal("42000.5"), 1710000000000L, "OKX");
        PriceTick tick2 = PriceTick.of("ETH-USDT-SWAP", new BigDecimal("42000.5"), 1710000000000L, "OKX");

        assertNotEquals(tick1, tick2);
    }

    @Test
    void testToString() {
        PriceTick tick = PriceTick.of("BTC-USDT-SWAP", new BigDecimal("42000.5"), 1710000000000L, "OKX");
        String str = tick.toString();

        assertNotNull(str);
        assertTrue(str.contains("BTC-USDT-SWAP"));
        assertTrue(str.contains("42000.5"));
    }
}

