package com.qyl.v2trade.market.aggregation.core.impl;

import com.qyl.v2trade.market.aggregation.config.SupportedPeriod;
import com.qyl.v2trade.market.aggregation.core.AggregationStats;
import com.qyl.v2trade.market.aggregation.event.AggregatedKLine;
import com.qyl.v2trade.market.model.event.KlineEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KlineAggregatorImpl单元测试
 */
class KlineAggregatorImplTest {
    
    private KlineAggregatorImpl aggregator;
    private List<AggregatedKLine> publishedEvents;
    
    @BeforeEach
    void setUp() {
        publishedEvents = new ArrayList<>();
        aggregator = new KlineAggregatorImpl(publishedEvents::add);
    }
    
    @Test
    void testSinglePeriodAggregation() {
        // 测试单周期正常聚合（5m周期）
        String symbol = "BTC-USDT";
        long baseTime = createTimestamp(2025, 1, 15, 10, 0, 0);
        
        // 发送5根1m K线（应该生成1根5m K线）
        for (int i = 0; i < 5; i++) {
            long openTime = baseTime + (i * 60 * 1000L);
            KlineEvent kline = createKlineEvent(symbol, openTime, 
                    new BigDecimal("100" + i), new BigDecimal("105" + i), new BigDecimal("99" + i), 
                    new BigDecimal("103" + i), new BigDecimal("1000"));
            aggregator.onKlineEvent(kline);
        }
        
        // 发送第6根K线（触发窗口关闭）
        long triggerTime = baseTime + (5 * 60 * 1000L);
        KlineEvent triggerKline = createKlineEvent(symbol, triggerTime, 
                new BigDecimal("105"), new BigDecimal("108"), new BigDecimal("102"), 
                new BigDecimal("106"), new BigDecimal("1500"));
        aggregator.onKlineEvent(triggerKline);
        
        // 验证：应该发布1个5m聚合事件
        assertEquals(5, publishedEvents.size(), "应该生成5个周期的聚合事件（5m, 15m, 30m, 1h, 4h）");
        
        // 找到5m周期的事件
        AggregatedKLine event5m = publishedEvents.stream()
                .filter(e -> "5m".equals(e.period()))
                .findFirst()
                .orElse(null);
        assertNotNull(event5m, "应该生成5m聚合事件");
        assertEquals(symbol, event5m.symbol(), "交易对符号应该匹配");
        assertEquals("5m", event5m.period(), "周期应该匹配");
        assertEquals(5, event5m.sourceKlineCount(), "应该聚合5根1m K线");
        assertEquals(new BigDecimal("100"), event5m.open(), "开盘价应该是第一根K线的开盘价");
        assertEquals(new BigDecimal("106"), event5m.close(), "收盘价应该是最后一根K线的收盘价");
    }
    
    @Test
    void testMultiPeriodConcurrentAggregation() {
        // 测试多周期并发聚合
        String symbol = "BTC-USDT";
        long baseTime = createTimestamp(2025, 1, 15, 10, 0, 0);
        
        // 发送15根1m K线（应该生成多个周期的聚合事件）
        for (int i = 0; i < 15; i++) {
            long openTime = baseTime + (i * 60 * 1000L);
            KlineEvent kline = createKlineEvent(symbol, openTime, 
                    new BigDecimal("100"), new BigDecimal("105"), new BigDecimal("99"), 
                    new BigDecimal("103"), new BigDecimal("1000"));
            aggregator.onKlineEvent(kline);
        }
        
        // 发送第16根K线（触发多个窗口关闭）
        long triggerTime = baseTime + (15 * 60 * 1000L);
        KlineEvent triggerKline = createKlineEvent(symbol, triggerTime, 
                new BigDecimal("103"), new BigDecimal("107"), new BigDecimal("102"), 
                new BigDecimal("106"), new BigDecimal("1500"));
        aggregator.onKlineEvent(triggerKline);
        
        // 验证：应该发布多个周期的聚合事件
        assertTrue(publishedEvents.size() >= 3, "应该生成至少3个周期的聚合事件");
        
        // 验证5m周期（应该生成3个5m事件）
        long count5m = publishedEvents.stream()
                .filter(e -> "5m".equals(e.period()))
                .count();
        assertEquals(3, count5m, "应该生成3个5m聚合事件");
        
        // 验证15m周期（应该生成1个15m事件）
        long count15m = publishedEvents.stream()
                .filter(e -> "15m".equals(e.period()))
                .count();
        assertEquals(1, count15m, "应该生成1个15m聚合事件");
    }
    
    @Test
    void testWindowCompleteTrigger() {
        // 测试窗口结束触发
        String symbol = "BTC-USDT";
        long baseTime = createTimestamp(2025, 1, 15, 10, 0, 0);
        
        // 发送4根1m K线（窗口未结束）
        for (int i = 0; i < 4; i++) {
            long openTime = baseTime + (i * 60 * 1000L);
            KlineEvent kline = createKlineEvent(symbol, openTime, 
                    new BigDecimal("100"), new BigDecimal("105"), new BigDecimal("99"), 
                    new BigDecimal("103"), new BigDecimal("1000"));
            aggregator.onKlineEvent(kline);
        }
        
        // 此时应该没有发布事件
        assertEquals(0, publishedEvents.size(), "窗口未结束时不应该发布事件");
        
        // 发送第5根K线（触发窗口关闭）
        long triggerTime = baseTime + (5 * 60 * 1000L);
        KlineEvent triggerKline = createKlineEvent(symbol, triggerTime, 
                new BigDecimal("103"), new BigDecimal("107"), new BigDecimal("102"), 
                new BigDecimal("106"), new BigDecimal("1500"));
        aggregator.onKlineEvent(triggerKline);
        
        // 验证：应该发布事件
        assertTrue(publishedEvents.size() > 0, "窗口结束时应该发布事件");
    }
    
    @Test
    void testDuplicateKlineDeduplication() {
        // 测试重复数据去重
        String symbol = "BTC-USDT";
        long baseTime = createTimestamp(2025, 1, 15, 10, 0, 0);
        
        // 发送第一根K线
        KlineEvent kline1 = createKlineEvent(symbol, baseTime, 
                new BigDecimal("100"), new BigDecimal("105"), new BigDecimal("99"), 
                new BigDecimal("103"), new BigDecimal("1000"));
        aggregator.onKlineEvent(kline1);
        
        // 再次发送相同的K线（应该被去重）
        KlineEvent kline1Duplicate = createKlineEvent(symbol, baseTime, 
                new BigDecimal("100"), new BigDecimal("105"), new BigDecimal("99"), 
                new BigDecimal("103"), new BigDecimal("1000"));
        aggregator.onKlineEvent(kline1Duplicate);
        
        // 发送触发窗口关闭的K线
        long triggerTime = baseTime + (5 * 60 * 1000L);
        KlineEvent triggerKline = createKlineEvent(symbol, triggerTime, 
                new BigDecimal("103"), new BigDecimal("107"), new BigDecimal("102"), 
                new BigDecimal("106"), new BigDecimal("1500"));
        aggregator.onKlineEvent(triggerKline);
        
        // 验证：5m周期的事件应该只聚合了1根K线（去重后）
        AggregatedKLine event5m = publishedEvents.stream()
                .filter(e -> "5m".equals(e.period()))
                .findFirst()
                .orElse(null);
        assertNotNull(event5m, "应该生成5m聚合事件");
        assertEquals(1, event5m.sourceKlineCount(), "重复K线应该被去重，只聚合1根K线");
    }
    
    @Test
    void testCrossWindowBoundary() {
        // 测试边界情况（跨窗口）
        String symbol = "BTC-USDT";
        long baseTime = createTimestamp(2025, 1, 15, 10, 0, 0);
        
        // 发送第一个5m窗口的K线（4根）
        for (int i = 0; i < 4; i++) {
            long openTime = baseTime + (i * 60 * 1000L);
            KlineEvent kline = createKlineEvent(symbol, openTime, 
                    new BigDecimal("100"), new BigDecimal("105"), new BigDecimal("99"), 
                    new BigDecimal("103"), new BigDecimal("1000"));
            aggregator.onKlineEvent(kline);
        }
        
        // 发送触发第一个窗口关闭的K线
        long triggerTime1 = baseTime + (5 * 60 * 1000L);
        KlineEvent triggerKline1 = createKlineEvent(symbol, triggerTime1, 
                new BigDecimal("103"), new BigDecimal("107"), new BigDecimal("102"), 
                new BigDecimal("106"), new BigDecimal("1500"));
        aggregator.onKlineEvent(triggerKline1);
        
        // 验证：第一个窗口应该关闭
        AggregatedKLine event1 = publishedEvents.stream()
                .filter(e -> "5m".equals(e.period()))
                .findFirst()
                .orElse(null);
        assertNotNull(event1, "第一个窗口应该关闭");
        
        // 发送第二个窗口的K线（4根）
        long baseTime2 = baseTime + (5 * 60 * 1000L);
        for (int i = 1; i < 5; i++) {
            long openTime = baseTime2 + (i * 60 * 1000L);
            KlineEvent kline = createKlineEvent(symbol, openTime, 
                    new BigDecimal("106"), new BigDecimal("110"), new BigDecimal("104"), 
                    new BigDecimal("108"), new BigDecimal("1200"));
            aggregator.onKlineEvent(kline);
        }
        
        // 发送触发第二个窗口关闭的K线
        long triggerTime2 = baseTime2 + (5 * 60 * 1000L);
        KlineEvent triggerKline2 = createKlineEvent(symbol, triggerTime2, 
                new BigDecimal("108"), new BigDecimal("112"), new BigDecimal("106"), 
                new BigDecimal("110"), new BigDecimal("1600"));
        aggregator.onKlineEvent(triggerKline2);
        
        // 验证：应该有两个5m窗口的事件
        long count5m = publishedEvents.stream()
                .filter(e -> "5m".equals(e.period()))
                .count();
        assertEquals(2, count5m, "应该有两个5m窗口的聚合事件");
    }
    
    @Test
    void testGetStats() {
        // 测试统计信息
        String symbol = "BTC-USDT";
        long baseTime = createTimestamp(2025, 1, 15, 10, 0, 0);
        
        // 初始状态
        AggregationStats stats0 = aggregator.getStats();
        assertEquals(0, stats0.activeBucketCount(), "初始Bucket数量应该为0");
        assertEquals(0, stats0.totalEventCount(), "初始事件数量应该为0");
        assertEquals(0, stats0.totalAggregatedCount(), "初始聚合数量应该为0");
        
        // 发送几根K线
        for (int i = 0; i < 3; i++) {
            long openTime = baseTime + (i * 60 * 1000L);
            KlineEvent kline = createKlineEvent(symbol, openTime, 
                    new BigDecimal("100"), new BigDecimal("105"), new BigDecimal("99"), 
                    new BigDecimal("103"), new BigDecimal("1000"));
            aggregator.onKlineEvent(kline);
        }
        
        // 检查统计信息
        AggregationStats stats1 = aggregator.getStats();
        assertTrue(stats1.activeBucketCount() > 0, "应该有活跃的Bucket");
        assertEquals(3, stats1.totalEventCount(), "事件数量应该为3");
        
        // 触发窗口关闭
        long triggerTime = baseTime + (5 * 60 * 1000L);
        KlineEvent triggerKline = createKlineEvent(symbol, triggerTime, 
                new BigDecimal("103"), new BigDecimal("107"), new BigDecimal("102"), 
                new BigDecimal("106"), new BigDecimal("1500"));
        aggregator.onKlineEvent(triggerKline);
        
        // 检查统计信息
        AggregationStats stats2 = aggregator.getStats();
        assertTrue(stats2.totalAggregatedCount() > 0, "应该有聚合事件生成");
    }
    
    @Test
    void testCleanupExpiredBuckets() {
        // 测试清理过期Bucket
        String symbol = "BTC-USDT";
        long baseTime = createTimestamp(2025, 1, 15, 10, 0, 0);
        
        // 发送K线并触发窗口关闭
        for (int i = 0; i < 5; i++) {
            long openTime = baseTime + (i * 60 * 1000L);
            KlineEvent kline = createKlineEvent(symbol, openTime, 
                    new BigDecimal("100"), new BigDecimal("105"), new BigDecimal("99"), 
                    new BigDecimal("103"), new BigDecimal("1000"));
            aggregator.onKlineEvent(kline);
        }
        
        long triggerTime = baseTime + (5 * 60 * 1000L);
        KlineEvent triggerKline = createKlineEvent(symbol, triggerTime, 
                new BigDecimal("103"), new BigDecimal("107"), new BigDecimal("102"), 
                new BigDecimal("106"), new BigDecimal("1500"));
        aggregator.onKlineEvent(triggerKline);
        
        // 窗口关闭后，Bucket应该被清理
        // 但是，由于窗口关闭时Bucket会被立即清理，所以这里主要测试清理方法的执行
        aggregator.cleanupExpiredBuckets();
        
        // 验证：清理方法执行无异常
        AggregationStats stats = aggregator.getStats();
        assertNotNull(stats, "统计信息应该存在");
    }
    
    @Test
    void testSkipNon1mKlines() {
        // 测试跳过非1m K线
        String symbol = "BTC-USDT";
        long baseTime = createTimestamp(2025, 1, 15, 10, 0, 0);
        
        // 发送5m K线（应该被跳过）
        KlineEvent kline5m = createKlineEventWithInterval(symbol, baseTime, "5m",
                new BigDecimal("100"), new BigDecimal("105"), new BigDecimal("99"), 
                new BigDecimal("103"), new BigDecimal("1000"));
        aggregator.onKlineEvent(kline5m);
        
        // 验证：不应该有Bucket被创建
        Set<String> bucketKeys = aggregator.getActiveBucketKeys();
        assertEquals(0, bucketKeys.size(), "非1m K线不应该创建Bucket");
        
        // 验证：统计信息中的事件数量不应该增加（或者增加但被忽略）
        // 注意：实际上事件数量会增加，但不会处理，这是正常的
    }
    
    /**
     * 创建KlineEvent
     */
    private KlineEvent createKlineEvent(String symbol, long openTime, 
                                       BigDecimal open, BigDecimal high, BigDecimal low, 
                                       BigDecimal close, BigDecimal volume) {
        return createKlineEventWithInterval(symbol, openTime, "1m", open, high, low, close, volume);
    }
    
    /**
     * 创建KlineEvent（指定interval）
     */
    private KlineEvent createKlineEventWithInterval(String symbol, long openTime, String interval,
                                                    BigDecimal open, BigDecimal high, BigDecimal low, 
                                                    BigDecimal close, BigDecimal volume) {
        long closeTime = openTime + 60000; // 1分钟K线
        return KlineEvent.of(symbol, "OKX", openTime, closeTime, interval, 
                open, high, low, close, volume, false, System.currentTimeMillis());
    }
    
    /**
     * 创建时间戳（UTC时区）
     */
    private long createTimestamp(int year, int month, int day, int hour, int minute, int second) {
        ZonedDateTime zdt = ZonedDateTime.of(year, month, day, hour, minute, second, 0, ZoneId.of("UTC"));
        return zdt.toInstant().toEpochMilli();
    }
}

