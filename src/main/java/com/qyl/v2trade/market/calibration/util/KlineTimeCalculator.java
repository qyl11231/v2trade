package com.qyl.v2trade.market.calibration.util;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * K线时间点计算工具类
 * 
 * <p>时间语义：所有时间戳都是UTC epoch millis，K线时间戳必须对齐到分钟起始点。
 */
public class KlineTimeCalculator {

    /**
     * 计算期望的K线时间戳列表（1分钟K线）
     * 
     * <p>按分钟对齐，生成从startTimestamp到endTimestamp的所有整分钟时间戳（UTC）。
     * 时间戳语义：UTC epoch millis，对齐到分钟起始点。
     * 
     * <p>时间边界：左闭右开区间 [startTimestamp, endTimestamp)
     * 
     * <p>重构说明：移除硬编码时区偏移，假设传入的参数已经是正确的 UTC 时间戳。
     * 如果上游时间戳有问题，应该修复上游，而不是在这里"打补丁"。
     * 
     * @param ostartTimestamp 开始时间戳（毫秒，UTC epoch millis，包含）
     * @param oendTimestamp 结束时间戳（毫秒，UTC epoch millis，不包含）
     * @return 时间戳列表（毫秒，UTC epoch millis，分钟起始点）
     */
    public static List<Long> calculateExpectedTimestamps(long ostartTimestamp, long oendTimestamp) {
        List<Long> timestamps = new ArrayList<>();

        // 重构：直接使用传入的 UTC 时间戳，不进行任何时区偏移计算
        Long startTimestamp = ostartTimestamp;
        Long endTimestamp = oendTimestamp;

        if (startTimestamp <= 0 || endTimestamp <= 0 || startTimestamp >= endTimestamp) {
            return timestamps;
        }

        // 将开始时间对齐到分钟起始点（向下取整）
        long alignedStart = alignToMinuteStart(startTimestamp);
        
        // 将结束时间对齐到分钟起始点（向上取整，但不包含）
        long alignedEnd = alignToMinuteStart(endTimestamp);
        // 如果endTimestamp不是整分钟，需要包含下一个整分钟
        if (endTimestamp % 60000 != 0) {
            alignedEnd += 60000;
        }

        // 生成所有整分钟时间戳（UTC epoch millis）
        long current = alignedStart;
        while (current < alignedEnd) {
            timestamps.add(current);
            current += 60000; // 加1分钟（毫秒）
        }

        return timestamps;
    }

    /**
     * 将时间戳对齐到分钟起始点（向下取整）
     * 
     * <p>时间语义：UTC epoch millis，对齐后仍然是UTC。
     * 
     * @param timestamp 原始时间戳（毫秒，UTC epoch millis）
     * @return 对齐后的时间戳（毫秒，UTC epoch millis，分钟起始点）
     */
    public static long alignToMinuteStart(long timestamp) {
        // 向下取整到分钟：去掉秒和毫秒部分
        return (timestamp / 60000) * 60000;
    }

    /**
     * 将时间戳转换为Instant（用于调试和日志）
     * 
     * @param timestamp 时间戳（毫秒，UTC epoch millis）
     * @return Instant (UTC)
     */
    public static Instant timestampToInstant(long timestamp) {
        return Instant.ofEpochMilli(timestamp);
    }
}

