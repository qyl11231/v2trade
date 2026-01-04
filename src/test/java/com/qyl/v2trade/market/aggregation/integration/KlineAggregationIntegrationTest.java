package com.qyl.v2trade.market.aggregation.integration;

import com.qyl.v2trade.market.aggregation.config.SupportedPeriod;
import com.qyl.v2trade.market.aggregation.core.KlineAggregator;
import com.qyl.v2trade.market.aggregation.event.AggregatedKLine;
import com.qyl.v2trade.market.aggregation.event.AggregationEventPublisher;
import com.qyl.v2trade.market.aggregation.persistence.AggregatedKLineStorageService;
import com.qyl.v2trade.market.model.event.KlineEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * K线聚合集成测试
 * 
 * <p>测试端到端流程：1m事件 -> 聚合 -> 事件发布 -> 数据库写入
 */
@SpringBootTest
@ActiveProfiles("test")
class KlineAggregationIntegrationTest {
    
    @Autowired
    private KlineAggregator klineAggregator;
    
    @Autowired(required = false)
    private AggregationEventPublisher aggregationEventPublisher;
    
    @Autowired(required = false)
    private AggregatedKLineStorageService storageService;
    
    private List<AggregatedKLine> publishedEvents;
    private CountDownLatch eventLatch;
    
    @BeforeEach
    void setUp() {
        publishedEvents = new ArrayList<>();
        eventLatch = new CountDownLatch(10); // 允许多个事件
        
        // 如果事件发布器存在，订阅事件
        if (aggregationEventPublisher instanceof com.qyl.v2trade.market.aggregation.event.impl.AggregationEventPublisherImpl) {
            ((com.qyl.v2trade.market.aggregation.event.impl.AggregationEventPublisherImpl) aggregationEventPublisher)
                    .subscribe(aggregatedKLine -> {
                        publishedEvents.add(aggregatedKLine);
                        eventLatch.countDown();
                    });
        }
    }
    
    @Test
    void testEndToEndFlow() throws InterruptedException {
        // 测试端到端流程：1m事件 -> 聚合 -> 事件发布 -> 数据库写入
        String symbol = "BTC-USDT-SWAP";
        long baseTime = createTimestamp(2025, 12, 31, 10, 0, 0);
        
        // 发送5根1m K线
        for (int i = 0; i < 5; i++) {
            long openTime = baseTime + (i * 60 * 1000L);
            KlineEvent kline = createKlineEvent(symbol, openTime, 
                    new BigDecimal("100" + i), new BigDecimal("105" + i), new BigDecimal("99" + i), 
                    new BigDecimal("103" + i), new BigDecimal("1000"));
            klineAggregator.onKlineEvent(kline);
        }
        
        // 发送第6根K线（触发窗口关闭）
        long triggerTime = baseTime + (5 * 60 * 1000L);
        KlineEvent triggerKline = createKlineEvent(symbol, triggerTime, 
                new BigDecimal("105"), new BigDecimal("108"), new BigDecimal("102"), 
                new BigDecimal("106"), new BigDecimal("1500"));
        klineAggregator.onKlineEvent(triggerKline);
        
        // 等待事件发布（最多等待2秒）
        boolean eventReceived = eventLatch.await(2, TimeUnit.SECONDS);
        
        // 验证：应该发布聚合事件
        if (aggregationEventPublisher != null) {
            assertTrue(eventReceived || !publishedEvents.isEmpty(), "应该发布聚合事件");
            
            // 找到5m周期的事件
            AggregatedKLine event5m = publishedEvents.stream()
                    .filter(e -> "5m".equals(e.period()))
                    .findFirst()
                    .orElse(null);
            
            if (event5m != null) {
                assertEquals(symbol, event5m.symbol(), "交易对符号应该匹配");
                assertEquals("5m", event5m.period(), "周期应该匹配");
                assertEquals(5, event5m.sourceKlineCount(), "应该聚合5根1m K线");
            }
        }
        
        // 验证：如果存储服务存在，应该写入数据库
        if (storageService != null && !publishedEvents.isEmpty()) {
            AggregatedKLine event5m = publishedEvents.stream()
                    .filter(e -> "5m".equals(e.period()))
                    .findFirst()
                    .orElse(null);
            
            if (event5m != null) {
                // 等待异步写入完成
                Thread.sleep(500);
                
                // 验证数据已写入
                boolean exists = storageService.exists(event5m.symbol(), event5m.period(), event5m.timestamp());
                assertTrue(exists, "聚合数据应该已写入数据库");
            }
        }
    }
    
    @Test
    void testMultiPeriodConcurrentAggregation() {
        // 测试多周期并发聚合
        String symbol = "BTC-USDT";
        long baseTime = createTimestamp(2025, 1, 15, 10, 0, 0);
        
        // 发送15根1m K线
        for (int i = 0; i < 15; i++) {
            long openTime = baseTime + (i * 60 * 1000L);
            KlineEvent kline = createKlineEvent(symbol, openTime, 
                    new BigDecimal("100"), new BigDecimal("105"), new BigDecimal("99"), 
                    new BigDecimal("103"), new BigDecimal("1000"));
            klineAggregator.onKlineEvent(kline);
        }
        
        // 发送第16根K线（触发多个窗口关闭）
        long triggerTime = baseTime + (15 * 60 * 1000L);
        KlineEvent triggerKline = createKlineEvent(symbol, triggerTime, 
                new BigDecimal("103"), new BigDecimal("107"), new BigDecimal("102"), 
                new BigDecimal("106"), new BigDecimal("1500"));
        klineAggregator.onKlineEvent(triggerKline);
        
        // 等待事件发布
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 验证：应该发布多个周期的聚合事件
        if (aggregationEventPublisher != null) {
            assertTrue(publishedEvents.size() >= 3, "应该生成至少3个周期的聚合事件");
            
            // 验证5m周期（应该生成3个5m事件）
            long count5m = publishedEvents.stream()
                    .filter(e -> "5m".equals(e.period()))
                    .count();
            assertTrue(count5m >= 3, "应该生成至少3个5m聚合事件");
        }
    }
    
    @Test
    void testStats() {
        // 测试统计信息
        var statsBefore = klineAggregator.getStats();
        assertNotNull(statsBefore, "统计信息应该存在");
        
        // 发送一些K线
        String symbol = "BTC-USDT";
        long baseTime = createTimestamp(2025, 1, 15, 10, 0, 0);
        
        for (int i = 0; i < 3; i++) {
            long openTime = baseTime + (i * 60 * 1000L);
            KlineEvent kline = createKlineEvent(symbol, openTime, 
                    new BigDecimal("100"), new BigDecimal("105"), new BigDecimal("99"), 
                    new BigDecimal("103"), new BigDecimal("1000"));
            klineAggregator.onKlineEvent(kline);
        }
        
        var statsAfter = klineAggregator.getStats();
        assertTrue(statsAfter.totalEventCount() > statsBefore.totalEventCount(), 
                "事件数量应该增加");
    }
    
    /**
     * 创建KlineEvent
     */
    private KlineEvent createKlineEvent(String symbol, long openTime, 
                                       BigDecimal open, BigDecimal high, BigDecimal low, 
                                       BigDecimal close, BigDecimal volume) {
        long closeTime = openTime + 60000; // 1分钟K线
        return KlineEvent.of(
                symbol, 
                "OKX", 
                openTime, 
                closeTime, 
                "1m", 
                open, 
                high, 
                low, 
                close, 
                volume, 
                false, 
                System.currentTimeMillis()
        );
    }
    
    /**
     * 创建时间戳（UTC时区）
     */
    private long createTimestamp(int year, int month, int day, int hour, int minute, int second) {
        ZonedDateTime zdt = ZonedDateTime.of(year, month, day, hour, minute, second, 0, ZoneId.of("UTC"));
        return zdt.toInstant().toEpochMilli();
    }
}

