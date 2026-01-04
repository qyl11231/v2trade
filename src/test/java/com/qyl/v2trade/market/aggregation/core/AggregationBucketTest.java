package com.qyl.v2trade.market.aggregation.core;

import com.qyl.v2trade.market.aggregation.config.SupportedPeriod;
import com.qyl.v2trade.market.aggregation.event.AggregatedKLine;
import com.qyl.v2trade.market.model.event.KlineEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AggregationBucket单元测试
 */
class AggregationBucketTest {
    
    private String symbol;
    private String period;
    private long windowStart;
    private long windowEnd;
    
    @BeforeEach
    void setUp() {
        symbol = "BTC-USDT";
        period = "5m";
        windowStart = createTimestamp(2025, 1, 15, 10, 0, 0);
        windowEnd = createTimestamp(2025, 1, 15, 10, 5, 0);
    }
    
    @Test
    void testNormalUpdateFlow() {
        // 测试正常更新流程
        AggregationBucket bucket = new AggregationBucket(symbol, period, windowStart, windowEnd);
        
        // 第一根K线
        KlineEvent kline1 = createKlineEvent(symbol, windowStart, 
                new BigDecimal("100"), new BigDecimal("105"), new BigDecimal("99"), 
                new BigDecimal("103"), new BigDecimal("1000"));
        boolean completed = bucket.update(kline1);
        assertFalse(completed, "第一根K线不应该触发窗口关闭");
        assertEquals(new BigDecimal("100"), bucket.getOpen(), "开盘价应该是第一根K线的开盘价");
        assertEquals(new BigDecimal("105"), bucket.getHigh(), "最高价应该是第一根K线的最高价");
        assertEquals(new BigDecimal("99"), bucket.getLow(), "最低价应该是第一根K线的最低价");
        assertEquals(new BigDecimal("103"), bucket.getClose(), "收盘价应该是第一根K线的收盘价");
        assertEquals(new BigDecimal("1000"), bucket.getVolume(), "成交量应该是第一根K线的成交量");
        assertEquals(1, bucket.getKlineCount(), "K线计数应该是1");
        
        // 第二根K线
        KlineEvent kline2 = createKlineEvent(symbol, windowStart + 60000, 
                new BigDecimal("103"), new BigDecimal("108"), new BigDecimal("102"), 
                new BigDecimal("106"), new BigDecimal("1500"));
        completed = bucket.update(kline2);
        assertFalse(completed, "第二根K线不应该触发窗口关闭");
        assertEquals(new BigDecimal("100"), bucket.getOpen(), "开盘价保持不变");
        assertEquals(new BigDecimal("108"), bucket.getHigh(), "最高价应该更新为108");
        assertEquals(new BigDecimal("99"), bucket.getLow(), "最低价保持99");
        assertEquals(new BigDecimal("106"), bucket.getClose(), "收盘价应该更新为106");
        assertEquals(new BigDecimal("2500"), bucket.getVolume(), "成交量应该是累计值");
        assertEquals(2, bucket.getKlineCount(), "K线计数应该是2");
    }
    
    @Test
    void testOHLCVCalculation() {
        // 测试OHLCV计算正确性
        AggregationBucket bucket = new AggregationBucket(symbol, period, windowStart, windowEnd);
        
        // 添加多根K线
        bucket.update(createKlineEvent(symbol, windowStart, 
                new BigDecimal("100"), new BigDecimal("105"), new BigDecimal("99"), 
                new BigDecimal("103"), new BigDecimal("1000")));
        bucket.update(createKlineEvent(symbol, windowStart + 60000, 
                new BigDecimal("103"), new BigDecimal("108"), new BigDecimal("102"), 
                new BigDecimal("106"), new BigDecimal("1500")));
        bucket.update(createKlineEvent(symbol, windowStart + 120000, 
                new BigDecimal("106"), new BigDecimal("107"), new BigDecimal("98"), 
                new BigDecimal("104"), new BigDecimal("2000")));
        
        // 验证OHLCV
        assertEquals(new BigDecimal("100"), bucket.getOpen(), "开盘价：第一根K线的开盘价");
        assertEquals(new BigDecimal("108"), bucket.getHigh(), "最高价：所有K线的最高价");
        assertEquals(new BigDecimal("98"), bucket.getLow(), "最低价：所有K线的最低价");
        assertEquals(new BigDecimal("104"), bucket.getClose(), "收盘价：最后一根K线的收盘价");
        assertEquals(new BigDecimal("4500"), bucket.getVolume(), "成交量：所有K线的成交量之和");
        assertEquals(3, bucket.getKlineCount(), "K线计数：3");
    }
    
    @Test
    void testWindowComplete() {
        // 测试窗口结束判断
        AggregationBucket bucket = new AggregationBucket(symbol, period, windowStart, windowEnd);
        
        // 添加窗口内的K线
        bucket.update(createKlineEvent(symbol, windowStart, 
                new BigDecimal("100"), new BigDecimal("105"), new BigDecimal("99"), 
                new BigDecimal("103"), new BigDecimal("1000")));
        
        // 添加触发窗口关闭的K线（openTime >= windowEnd）
        KlineEvent klineEnd = createKlineEvent(symbol, windowEnd, 
                new BigDecimal("103"), new BigDecimal("107"), new BigDecimal("102"), 
                new BigDecimal("106"), new BigDecimal("1500"));
        boolean completed = bucket.update(klineEnd);
        
        assertTrue(completed, "应该触发窗口关闭");
        assertTrue(bucket.isComplete(), "Bucket应该标记为已完成");
        assertTrue(bucket.isWindowComplete(windowEnd), "窗口结束判断应该返回true");
    }
    
    @Test
    void testToAggregatedKLine() {
        // 测试生成聚合K线事件
        AggregationBucket bucket = new AggregationBucket(symbol, period, windowStart, windowEnd);
        
        // 添加K线数据
        bucket.update(createKlineEvent(symbol, windowStart, 
                new BigDecimal("100"), new BigDecimal("105"), new BigDecimal("99"), 
                new BigDecimal("103"), new BigDecimal("1000")));
        bucket.update(createKlineEvent(symbol, windowStart + 60000, 
                new BigDecimal("103"), new BigDecimal("108"), new BigDecimal("102"), 
                new BigDecimal("106"), new BigDecimal("1500")));
        
        // 生成聚合结果
        AggregatedKLine aggregated = bucket.toAggregatedKLine();
        assertNotNull(aggregated, "应该能生成聚合K线");
        assertEquals(symbol, aggregated.symbol(), "交易对符号应该匹配");
        assertEquals(period, aggregated.period(), "周期应该匹配");
        
        // 验证时间戳对齐（应该对齐到windowStart）
        long alignedTimestamp = PeriodCalculator.alignTimestamp(windowStart, SupportedPeriod.M5);
        assertEquals(alignedTimestamp, aggregated.timestamp(), "时间戳应该对齐到窗口起始时间");
        
        // 验证OHLCV
        assertEquals(new BigDecimal("100"), aggregated.open(), "开盘价应该匹配");
        assertEquals(new BigDecimal("108"), aggregated.high(), "最高价应该匹配");
        assertEquals(new BigDecimal("99"), aggregated.low(), "最低价应该匹配");
        assertEquals(new BigDecimal("106"), aggregated.close(), "收盘价应该匹配");
        assertEquals(new BigDecimal("2500"), aggregated.volume(), "成交量应该匹配");
        assertEquals(2, aggregated.sourceKlineCount(), "源K线数量应该匹配");
    }
    
    @Test
    void testToAggregatedKLine_EmptyBucket() {
        // 测试空Bucket不生成聚合结果
        AggregationBucket bucket = new AggregationBucket(symbol, period, windowStart, windowEnd);
        AggregatedKLine aggregated = bucket.toAggregatedKLine();
        assertNull(aggregated, "空Bucket不应该生成聚合结果");
    }
    
    @Test
    void testUpdateAfterComplete() {
        // 测试窗口关闭后不能再更新
        AggregationBucket bucket = new AggregationBucket(symbol, period, windowStart, windowEnd);
        
        // 触发窗口关闭
        bucket.update(createKlineEvent(symbol, windowEnd, 
                new BigDecimal("100"), new BigDecimal("105"), new BigDecimal("99"), 
                new BigDecimal("103"), new BigDecimal("1000")));
        
        // 尝试再次更新应该抛出异常
        assertThrows(IllegalStateException.class, () -> {
            bucket.update(createKlineEvent(symbol, windowEnd + 60000, 
                    new BigDecimal("103"), new BigDecimal("107"), new BigDecimal("102"), 
                    new BigDecimal("106"), new BigDecimal("1500")));
        }, "窗口关闭后应该不能继续更新");
    }
    
    @Test
    void testIsExpired() {
        // 测试过期判断
        AggregationBucket bucket = new AggregationBucket(symbol, period, windowStart, windowEnd);
        
        // 未完成的窗口不过期
        assertFalse(bucket.isExpired(System.currentTimeMillis()), "未完成的窗口不应该过期");
        
        // 完成窗口
        bucket.update(createKlineEvent(symbol, windowEnd, 
                new BigDecimal("100"), new BigDecimal("105"), new BigDecimal("99"), 
                new BigDecimal("103"), new BigDecimal("1000")));
        
        // 窗口结束后1小时内不过期
        long currentTime = windowEnd + (30 * 60 * 1000L); // 30分钟后
        assertFalse(bucket.isExpired(currentTime), "窗口结束30分钟后不应该过期");
        
        // 窗口结束超过1小时后过期
        currentTime = windowEnd + (61 * 60 * 1000L); // 61分钟后
        assertTrue(bucket.isExpired(currentTime), "窗口结束61分钟后应该过期");
    }
    
    @Test
    void testThreadSafety() throws InterruptedException {
        // 测试线程安全
        AggregationBucket bucket = new AggregationBucket(symbol, period, windowStart, windowEnd);
        
        int threadCount = 10;
        int updatesPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger exceptionCount = new AtomicInteger(0);
        
        // 多线程并发更新
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < updatesPerThread; j++) {
                        long timestamp = windowStart + (threadId * updatesPerThread + j) * 1000L;
                        if (timestamp < windowEnd) {
                            bucket.update(createKlineEvent(symbol, timestamp, 
                                    new BigDecimal("100"), new BigDecimal("105"), new BigDecimal("99"), 
                                    new BigDecimal("103"), new BigDecimal("100")));
                        }
                    }
                } catch (Exception e) {
                    exceptionCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        // 验证没有异常（除了窗口关闭后的更新）
        assertEquals(0, exceptionCount.get(), "不应该有并发异常");
        
        // 验证数据完整性
        assertTrue(bucket.getKlineCount() > 0, "应该有K线数据");
        assertNotNull(bucket.getOpen(), "开盘价不应该为null");
    }
    
    /**
     * 创建KlineEvent
     */
    private KlineEvent createKlineEvent(String symbol, long openTime, 
                                       BigDecimal open, BigDecimal high, BigDecimal low, 
                                       BigDecimal close, BigDecimal volume) {
        long closeTime = openTime + 60000; // 1分钟K线
        return KlineEvent.of(symbol, "OKX", openTime, closeTime, "1m", 
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

