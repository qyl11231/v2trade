package com.qyl.v2trade.common.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * UTC时间转换工具类
 * 
 * <p>核心功能：
 * <ul>
 *   <li>将本地时间（如北京时间 UTC+8）转换为UTC时间戳</li>
 *   <li>将UTC时间戳转换为本地时间字符串（用于显示）</li>
 *   <li>时间范围转换（本地时间范围 → UTC时间范围）</li>
 *   <li>时间对齐（对齐到分钟起始点）</li>
 * </ul>
 * 
 * <p>时间语义：
 * <ul>
 *   <li>所有时间戳都是UTC epoch millis（毫秒）</li>
 *   <li>K线时间戳必须对齐到分钟起始点</li>
 *   <li>查询边界使用左闭右开区间 [start, end)</li>
 * </ul>
 *
 * @author qyl
 */
public class UtcTimeConverter {

    /**
     * 默认本地时区：北京时间（UTC+8）
     */
    public static final String DEFAULT_LOCAL_TIMEZONE = "Asia/Shanghai";

    /**
     * 时间格式化器：yyyy-MM-dd HH:mm:ss
     */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 将本地时间字符串转换为UTC时间戳（epoch millis）
     * 
     * <p>示例：
     * <pre>
     * 输入：北京时间 "2025-12-28 00:00:01"
     * 输出：UTC时间戳 1735344001000 (对应 UTC 2025-12-27 16:00:01)
     * </pre>
     * 
     * @param localTimeStr 本地时间字符串，格式：yyyy-MM-dd HH:mm:ss
     * @param timezone 时区，默认：Asia/Shanghai (UTC+8)
     * @return UTC时间戳（毫秒）
     */
    public static long localTimeToUtcTimestamp(String localTimeStr, String timezone) {
        if (localTimeStr == null || localTimeStr.trim().isEmpty()) {
            throw new IllegalArgumentException("本地时间字符串不能为空");
        }

        if (timezone == null || timezone.trim().isEmpty()) {
            timezone = DEFAULT_LOCAL_TIMEZONE;
        }

        try {
            // 解析本地时间字符串
            LocalDateTime localDateTime = LocalDateTime.parse(localTimeStr.trim(), TIME_FORMATTER);
            
            // 转换为指定时区的ZonedDateTime
            ZoneId zoneId = ZoneId.of(timezone);
            ZonedDateTime zonedDateTime = localDateTime.atZone(zoneId);
            
            // 转换为UTC时间戳
            return zonedDateTime.toInstant().toEpochMilli();
        } catch (Exception e) {
            throw new IllegalArgumentException("时间格式错误: " + localTimeStr + ", 期望格式: yyyy-MM-dd HH:mm:ss", e);
        }
    }

    /**
     * 将本地时间字符串转换为UTC时间戳（使用默认时区）
     * 
     * @param localTimeStr 本地时间字符串，格式：yyyy-MM-dd HH:mm:ss
     * @return UTC时间戳（毫秒）
     */
    public static long localTimeToUtcTimestamp(String localTimeStr) {
        return localTimeToUtcTimestamp(localTimeStr, DEFAULT_LOCAL_TIMEZONE);
    }

    /**
     * UTC时间范围对象
     */
    public static class UtcTimeRange {
        private final long startUtcTimestamp;
        private final long endUtcTimestamp;
        private final String startUtcString;
        private final String endUtcString;
        private final String startLocalString;
        private final String endLocalString;

        public UtcTimeRange(long startUtcTimestamp, long endUtcTimestamp, 
                           String startUtcString, String endUtcString,
                           String startLocalString, String endLocalString) {
            this.startUtcTimestamp = startUtcTimestamp;
            this.endUtcTimestamp = endUtcTimestamp;
            this.startUtcString = startUtcString;
            this.endUtcString = endUtcString;
            this.startLocalString = startLocalString;
            this.endLocalString = endLocalString;
        }

        public long getStartUtcTimestamp() {
            return startUtcTimestamp;
        }

        public long getEndUtcTimestamp() {
            return endUtcTimestamp;
        }

        public String getStartUtcString() {
            return startUtcString;
        }

        public String getEndUtcString() {
            return endUtcString;
        }

        public String getStartLocalString() {
            return startLocalString;
        }

        public String getEndLocalString() {
            return endLocalString;
        }
    }

    /**
     * 将本地时间范围转换为UTC时间范围，并对齐到分钟起始点
     * 
     * <p>示例：
     * <pre>
     * 输入：北京时间范围 "2025-12-28 00:00:01" - "2025-12-29 00:00:00"
     * 输出：UTC时间范围（已对齐）
     *   start: UTC 2025-12-27 16:00:00 (对齐到分钟起始点)
     *   end:   UTC 2025-12-28 16:00:00 (对齐到分钟起始点)
     * </pre>
     * 
     * @param localStartStr 本地开始时间字符串，格式：yyyy-MM-dd HH:mm:ss
     * @param localEndStr 本地结束时间字符串，格式：yyyy-MM-dd HH:mm:ss
     * @param timezone 时区，默认：Asia/Shanghai
     * @return UTC时间范围对象（已对齐到分钟起始点）
     */
    public static UtcTimeRange localTimeRangeToUtc(String localStartStr, String localEndStr, String timezone) {
        if (timezone == null || timezone.trim().isEmpty()) {
            timezone = DEFAULT_LOCAL_TIMEZONE;
        }

        // 转换为UTC时间戳
        long startUtcTimestamp = localTimeToUtcTimestamp(localStartStr, timezone);
        long endUtcTimestamp = localTimeToUtcTimestamp(localEndStr, timezone);

        // 对齐到分钟起始点
        long alignedStart = alignToMinuteStart(startUtcTimestamp);
        
        // 对齐结束时间：如果原始结束时间不是整分钟，需要包含下一个整分钟
        long alignedEnd = alignToMinuteStart(endUtcTimestamp);
        if (endUtcTimestamp % 60000 != 0) {
            alignedEnd += 60000;
        }

        // 格式化为字符串（用于日志）
        String startUtcString = utcTimestampToUtcString(alignedStart);
        String endUtcString = utcTimestampToUtcString(alignedEnd);
        String startLocalString = utcTimestampToLocalString(alignedStart, timezone);
        String endLocalString = utcTimestampToLocalString(alignedEnd, timezone);

        return new UtcTimeRange(alignedStart, alignedEnd, 
                               startUtcString, endUtcString,
                               startLocalString, endLocalString);
    }

    /**
     * 将本地时间范围转换为UTC时间范围（使用默认时区）
     * 
     * @param localStartStr 本地开始时间字符串
     * @param localEndStr 本地结束时间字符串
     * @return UTC时间范围对象
     */
    public static UtcTimeRange localTimeRangeToUtc(String localStartStr, String localEndStr) {
        return localTimeRangeToUtc(localStartStr, localEndStr, DEFAULT_LOCAL_TIMEZONE);
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
     * 将UTC时间戳转换为UTC时间字符串（用于日志）
     * 
     * @param utcTimestamp UTC时间戳（毫秒）
     * @return UTC时间字符串，格式：yyyy-MM-dd HH:mm:ss
     */
    public static String utcTimestampToUtcString(long utcTimestamp) {
        Instant instant = Instant.ofEpochMilli(utcTimestamp);
        ZonedDateTime utcTime = instant.atZone(ZoneId.of("UTC"));
        return utcTime.format(TIME_FORMATTER);
    }

    /**
     * 将UTC时间戳转换为本地时间字符串（用于显示和日志）
     * 
     * @param utcTimestamp UTC时间戳（毫秒）
     * @param timezone 时区，默认：Asia/Shanghai
     * @return 本地时间字符串，格式：yyyy-MM-dd HH:mm:ss
     */
    public static String utcTimestampToLocalString(long utcTimestamp, String timezone) {
        if (timezone == null || timezone.trim().isEmpty()) {
            timezone = DEFAULT_LOCAL_TIMEZONE;
        }

        Instant instant = Instant.ofEpochMilli(utcTimestamp);
        ZoneId zoneId = ZoneId.of(timezone);
        ZonedDateTime localTime = instant.atZone(zoneId);
        return localTime.format(TIME_FORMATTER);
    }

    /**
     * 将UTC时间戳转换为本地时间字符串（使用默认时区）
     * 
     * @param utcTimestamp UTC时间戳（毫秒）
     * @return 本地时间字符串
     */
    public static String utcTimestampToLocalString(long utcTimestamp) {
        return utcTimestampToLocalString(utcTimestamp, DEFAULT_LOCAL_TIMEZONE);
    }

    /**
     * 同时显示UTC和本地时间（用于日志）
     * 
     * <p>格式：timestamp (UTC: yyyy-MM-dd HH:mm:ss, CST: yyyy-MM-dd HH:mm:ss)
     * 
     * @param utcTimestamp UTC时间戳（毫秒）
     * @param timezone 时区，默认：Asia/Shanghai
     * @return 格式化的时间字符串
     */
    public static String formatWithBothTimezones(long utcTimestamp, String timezone) {
        String utcStr = utcTimestampToUtcString(utcTimestamp);
        String localStr = utcTimestampToLocalString(utcTimestamp, timezone);
        return String.format("%d (UTC: %s, CST: %s)", utcTimestamp, utcStr, localStr);
    }

    /**
     * 同时显示UTC和本地时间（使用默认时区）
     * 
     * @param utcTimestamp UTC时间戳（毫秒）
     * @return 格式化的时间字符串
     */
    public static String formatWithBothTimezones(long utcTimestamp) {
        return formatWithBothTimezones(utcTimestamp, DEFAULT_LOCAL_TIMEZONE);
    }

    /**
     * 验证时间范围有效性
     * 
     * @param startTimestamp 开始时间戳（毫秒）
     * @param endTimestamp 结束时间戳（毫秒）
     * @return true表示有效，false表示无效
     */
    public static boolean validateTimeRange(long startTimestamp, long endTimestamp) {
        return startTimestamp > 0 && endTimestamp > 0 && startTimestamp < endTimestamp;
    }
}

