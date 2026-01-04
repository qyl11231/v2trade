package com.qyl.v2trade.market.aggregation.core;

import com.qyl.v2trade.market.aggregation.config.SupportedPeriod;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PeriodCalculator单元测试
 */
class PeriodCalculatorTest {
    
    /**
     * 将时间戳转换为可读字符串（用于调试）
     */
    private String timestampToString(long timestamp) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.of("UTC"))
                .toString();
    }
    
    @Test
    void testCalculateWindowStart_5m() {
        // 测试5分钟周期窗口计算
        // 10:03:00 -> 10:00:00
        long timestamp = createTimestamp(2025, 1, 15, 10, 3, 0);
        long windowStart = PeriodCalculator.calculateWindowStart(timestamp, SupportedPeriod.M5);
        long expected = createTimestamp(2025, 1, 15, 10, 0, 0);
        assertEquals(expected, windowStart, "5m周期：10:03应该对齐到10:00");
        
        // 10:07:00 -> 10:05:00
        timestamp = createTimestamp(2025, 1, 15, 10, 7, 0);
        windowStart = PeriodCalculator.calculateWindowStart(timestamp, SupportedPeriod.M5);
        expected = createTimestamp(2025, 1, 15, 10, 5, 0);
        assertEquals(expected, windowStart, "5m周期：10:07应该对齐到10:05");
        
        // 10:05:00 -> 10:05:00（边界值）
        timestamp = createTimestamp(2025, 1, 15, 10, 5, 0);
        windowStart = PeriodCalculator.calculateWindowStart(timestamp, SupportedPeriod.M5);
        expected = createTimestamp(2025, 1, 15, 10, 5, 0);
        assertEquals(expected, windowStart, "5m周期：10:05应该对齐到10:05（边界值）");
    }
    
    @Test
    void testCalculateWindowStart_15m() {
        // 测试15分钟周期窗口计算
        // 10:17:00 -> 10:15:00
        long timestamp = createTimestamp(2025, 1, 15, 10, 17, 0);
        long windowStart = PeriodCalculator.calculateWindowStart(timestamp, SupportedPeriod.M15);
        long expected = createTimestamp(2025, 1, 15, 10, 15, 0);
        assertEquals(expected, windowStart, "15m周期：10:17应该对齐到10:15");
        
        // 10:30:00 -> 10:30:00（边界值）
        timestamp = createTimestamp(2025, 1, 15, 10, 30, 0);
        windowStart = PeriodCalculator.calculateWindowStart(timestamp, SupportedPeriod.M15);
        expected = createTimestamp(2025, 1, 15, 10, 30, 0);
        assertEquals(expected, windowStart, "15m周期：10:30应该对齐到10:30（边界值）");
    }
    
    @Test
    void testCalculateWindowStart_30m() {
        // 测试30分钟周期窗口计算
        // 10:33:00 -> 10:30:00
        long timestamp = createTimestamp(2025, 1, 15, 10, 33, 0);
        long windowStart = PeriodCalculator.calculateWindowStart(timestamp, SupportedPeriod.M30);
        long expected = createTimestamp(2025, 1, 15, 10, 30, 0);
        assertEquals(expected, windowStart, "30m周期：10:33应该对齐到10:30");
    }
    
    @Test
    void testCalculateWindowStart_1h() {
        // 测试1小时周期窗口计算
        // 10:45:00 -> 10:00:00
        long timestamp = createTimestamp(2025, 1, 15, 10, 45, 0);
        long windowStart = PeriodCalculator.calculateWindowStart(timestamp, SupportedPeriod.H1);
        long expected = createTimestamp(2025, 1, 15, 10, 0, 0);
        assertEquals(expected, windowStart, "1h周期：10:45应该对齐到10:00");
        
        // 11:00:00 -> 11:00:00（边界值）
        timestamp = createTimestamp(2025, 1, 15, 11, 0, 0);
        windowStart = PeriodCalculator.calculateWindowStart(timestamp, SupportedPeriod.H1);
        expected = createTimestamp(2025, 1, 15, 11, 0, 0);
        assertEquals(expected, windowStart, "1h周期：11:00应该对齐到11:00（边界值）");
    }
    
    @Test
    void testCalculateWindowStart_4h() {
        // 测试4小时周期窗口计算
        // 14:30:00 -> 12:00:00
        long timestamp = createTimestamp(2025, 1, 15, 14, 30, 0);
        long windowStart = PeriodCalculator.calculateWindowStart(timestamp, SupportedPeriod.H4);
        long expected = createTimestamp(2025, 1, 15, 12, 0, 0);
        assertEquals(expected, windowStart, "4h周期：14:30应该对齐到12:00");
        
        // 04:00:00 -> 04:00:00（边界值）
        timestamp = createTimestamp(2025, 1, 15, 4, 0, 0);
        windowStart = PeriodCalculator.calculateWindowStart(timestamp, SupportedPeriod.H4);
        expected = createTimestamp(2025, 1, 15, 4, 0, 0);
        assertEquals(expected, windowStart, "4h周期：04:00应该对齐到04:00（边界值）");
    }
    
    @Test
    void testCalculateWindowEnd() {
        // 测试窗口结束时间计算
        long windowStart = createTimestamp(2025, 1, 15, 10, 0, 0);
        
        // 5m周期：10:00 -> 10:05
        long windowEnd = PeriodCalculator.calculateWindowEnd(windowStart, SupportedPeriod.M5);
        long expected = createTimestamp(2025, 1, 15, 10, 5, 0);
        assertEquals(expected, windowEnd, "5m周期窗口结束时间应该是起始时间+5分钟");
        
        // 15m周期：10:00 -> 10:15
        windowEnd = PeriodCalculator.calculateWindowEnd(windowStart, SupportedPeriod.M15);
        expected = createTimestamp(2025, 1, 15, 10, 15, 0);
        assertEquals(expected, windowEnd, "15m周期窗口结束时间应该是起始时间+15分钟");
        
        // 1h周期：10:00 -> 11:00
        windowEnd = PeriodCalculator.calculateWindowEnd(windowStart, SupportedPeriod.H1);
        expected = createTimestamp(2025, 1, 15, 11, 0, 0);
        assertEquals(expected, windowEnd, "1h周期窗口结束时间应该是起始时间+1小时");
    }
    
    @Test
    void testAlignTimestamp() {
        // 测试时间戳对齐
        // alignTimestamp应该和calculateWindowStart结果一致
        long timestamp = createTimestamp(2025, 1, 15, 10, 17, 0);
        
        long aligned = PeriodCalculator.alignTimestamp(timestamp, SupportedPeriod.M15);
        long expected = PeriodCalculator.calculateWindowStart(timestamp, SupportedPeriod.M15);
        assertEquals(expected, aligned, "alignTimestamp应该和calculateWindowStart结果一致");
    }
    
    @Test
    void testBoundaryCase_Midnight() {
        // 测试边界情况：跨天（午夜）
        // 00:03:00 -> 00:00:00（5m周期）
        long timestamp = createTimestamp(2025, 1, 16, 0, 3, 0);
        long windowStart = PeriodCalculator.calculateWindowStart(timestamp, SupportedPeriod.M5);
        long expected = createTimestamp(2025, 1, 16, 0, 0, 0);
        assertEquals(expected, windowStart, "跨天边界：00:03应该对齐到00:00");
    }
    
    @Test
    void testBoundaryCase_HourBoundary() {
        // 测试边界情况：整点
        // 10:00:00 -> 10:00:00（所有周期）
        long timestamp = createTimestamp(2025, 1, 15, 10, 0, 0);
        
        long windowStart5m = PeriodCalculator.calculateWindowStart(timestamp, SupportedPeriod.M5);
        assertEquals(timestamp, windowStart5m, "5m周期：整点应该对齐到自身");
        
        long windowStart15m = PeriodCalculator.calculateWindowStart(timestamp, SupportedPeriod.M15);
        assertEquals(timestamp, windowStart15m, "15m周期：整点应该对齐到自身");
        
        long windowStart1h = PeriodCalculator.calculateWindowStart(timestamp, SupportedPeriod.H1);
        assertEquals(timestamp, windowStart1h, "1h周期：整点应该对齐到自身");
    }
    
    /**
     * 创建时间戳（UTC时区）
     */
    private long createTimestamp(int year, int month, int day, int hour, int minute, int second) {
        ZonedDateTime zdt = ZonedDateTime.of(year, month, day, hour, minute, second, 0, ZoneId.of("UTC"));
        return zdt.toInstant().toEpochMilli();
    }
}

