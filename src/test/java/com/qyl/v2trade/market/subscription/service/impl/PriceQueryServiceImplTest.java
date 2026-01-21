package com.qyl.v2trade.market.subscription.service.impl;

import com.qyl.v2trade.market.model.event.PriceTick;
import com.qyl.v2trade.market.subscription.collector.eventbus.PriceEventBus;
import com.qyl.v2trade.market.subscription.collector.eventbus.impl.SimplePriceEventBus;
import com.qyl.v2trade.market.subscription.service.LatestPrice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * PriceQueryServiceImpl 测试
 */
class PriceQueryServiceImplTest {

    private PriceQueryServiceImpl queryService;
    private LatestPriceServiceImpl latestPriceService;
    private org.springframework.context.ApplicationEventPublisher eventPublisher;
    private PriceEventBus priceEventBus;

    @BeforeEach
    void setUp() {
        eventPublisher = mock(org.springframework.context.ApplicationEventPublisher.class);
        priceEventBus = new SimplePriceEventBus();
        latestPriceService = new LatestPriceServiceImpl(eventPublisher, priceEventBus);
        queryService = new PriceQueryServiceImpl(latestPriceService);
    }

    @Test
    void testGetLatestPrice() {
        latestPriceService.updatePrice(PriceTick.of("BTC-USDT-SWAP", new BigDecimal("42000.5"), 1710000000000L, "OKX"));

        Optional<BigDecimal> result = queryService.getLatestPrice("BTC-USDT-SWAP");

        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("42000.5"), result.get());
    }

    @Test
    void testGetLatestPriceNotFound() {
        Optional<BigDecimal> result = queryService.getLatestPrice("BTC-USDT-SWAP");
        assertFalse(result.isPresent());
    }

    @Test
    void testGetLatestPriceNull() {
        Optional<BigDecimal> result = queryService.getLatestPrice(null);
        assertFalse(result.isPresent());
    }

    @Test
    void testGetLatestPriceWithTimestamp() {
        latestPriceService.updatePrice(PriceTick.of("BTC-USDT-SWAP", new BigDecimal("42000.5"), 1710000000000L, "OKX"));

        Optional<LatestPrice> result = queryService.getLatestPriceWithTimestamp("BTC-USDT-SWAP");

        assertTrue(result.isPresent());
        assertEquals("BTC-USDT-SWAP", result.get().symbol());
        assertEquals(new BigDecimal("42000.5"), result.get().price());
        assertEquals(1710000000000L, result.get().timestamp());
    }

    @Test
    void testGetLatestPriceWithTimestampNotFound() {
        Optional<LatestPrice> result = queryService.getLatestPriceWithTimestamp("BTC-USDT-SWAP");
        assertFalse(result.isPresent());
    }

    @Test
    void testGetLatestPrices() {
        latestPriceService.updatePrice(PriceTick.of("BTC-USDT-SWAP", new BigDecimal("42000.5"), 1710000000000L, "OKX"));
        latestPriceService.updatePrice(PriceTick.of("ETH-USDT-SWAP", new BigDecimal("3000.0"), 1710000000000L, "OKX"));

        Map<String, BigDecimal> result = queryService.getLatestPrices(Set.of("BTC-USDT-SWAP", "ETH-USDT-SWAP", "UNKNOWN"));

        assertEquals(2, result.size());
        assertEquals(new BigDecimal("42000.5"), result.get("BTC-USDT-SWAP"));
        assertEquals(new BigDecimal("3000.0"), result.get("ETH-USDT-SWAP"));
        assertFalse(result.containsKey("UNKNOWN"));
    }

    @Test
    void testGetLatestPricesEmpty() {
        Map<String, BigDecimal> result = queryService.getLatestPrices(Set.of());
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetLatestPricesNull() {
        Map<String, BigDecimal> result = queryService.getLatestPrices(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void testHasPrice() {
        latestPriceService.updatePrice(PriceTick.of("BTC-USDT-SWAP", new BigDecimal("42000.5"), 1710000000000L, "OKX"));

        assertTrue(queryService.hasPrice("BTC-USDT-SWAP"));
        assertFalse(queryService.hasPrice("ETH-USDT-SWAP"));
    }

    @Test
    void testHasPriceNull() {
        assertFalse(queryService.hasPrice(null));
    }
}

