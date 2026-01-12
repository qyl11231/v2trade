package com.qyl.v2trade.common.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 统一时间管理工具类
 * 
 * <p>本工具类是整个系统中唯一负责时区转换和时间格式化的权威工具。
 * 所有时间相关的操作都应该通过本类完成，以确保时间处理的一致性和正确性。
 * 
 * <p><b>核心设计原则：</b>
 * <ul>
 *   <li>系统内部统一使用 {@link Instant} 表示时间点（UTC）</li>
 *   <li>时区转换仅在系统边界（Controller/DTO层）进行</li>
 *   <li>严禁在业务逻辑中进行硬编码的时区偏移计算</li>
 * </ul>
 * 
 * <p><b>时间语义规范：</b>
 * <ul>
 *   <li>数据库（QuestDB, MySQL）存储的时间戳均为 UTC</li>
 *   <li>OKX API 交互使用的时间戳为 UTC epoch millis</li>
 *   <li>前端展示时间为用户本地时区（默认为 Asia/Shanghai）</li>
 * </ul>
 *
 * @author Manus AI 量化架构组
 * @version 2.0
 * @since 2026-01-11
 */
public final class TimeUtil {
    
    private TimeUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * UTC 时区常量
     */
    public static final ZoneId ZONE_UTC = ZoneId.of("UTC");
    
    /**
     * 上海时区常量（东八区，UTC+8）
     */
    public static final ZoneId ZONE_SHANGHAI = ZoneId.of("Asia/Shanghai");
    
    /**
     * 标准日期时间格式化器（用于展示和日志）
     */
    private static final DateTimeFormatter STANDARD_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== 核心转换方法 ====================

    /**
     * 将毫秒时间戳安全地转换为 Instant
     * 
     * <p>用于从 OKX API 或其他外部系统接收到的 long 类型时间戳。
     * 
     * @param epochMilli UTC 毫秒时间戳
     * @return 对应的 Instant 对象
     * @throws IllegalArgumentException 如果时间戳为负数
     */
    public static Instant fromEpochMilli(long epochMilli) {
        if (epochMilli < 0) {
            throw new IllegalArgumentException("时间戳不能为负数: " + epochMilli);
        }
        return Instant.ofEpochMilli(epochMilli);
    }

    /**
     * 将 Instant 转换为毫秒时间戳
     * 
     * <p>用于调用 OKX API 或其他需要 long 类型时间戳的场景。
     * 
     * @param instant Instant 对象
     * @return UTC 毫秒时间戳
     * @throws NullPointerException 如果 instant 为 null
     */
    public static long toEpochMilli(Instant instant) {
        if (instant == null) {
            throw new NullPointerException("Instant 不能为 null");
        }
        return instant.toEpochMilli();
    }

    // ==================== 格式化方法 ====================

    /**
     * 将 UTC Instant 格式化为上海时间字符串
     * 
     * <p>用于前端展示或日志输出。
     * 格式：yyyy-MM-dd HH:mm:ss
     * 
     * @param instant UTC 时间点
     * @return 上海时区的时间字符串，例如 "2026-01-11 10:00:00"
     */
    public static String formatAsShanghaiString(Instant instant) {
        if (instant == null) {
            return null;
        }
        ZonedDateTime shanghaiTime = instant.atZone(ZONE_SHANGHAI);
        return shanghaiTime.format(STANDARD_FORMATTER);
    }

    /**
     * 将 UTC Instant 格式化为 UTC 时间字符串
     * 
     * <p>用于日志输出或调试。
     * 格式：yyyy-MM-dd HH:mm:ss
     * 
     * @param instant UTC 时间点
     * @return UTC 时间字符串，例如 "2026-01-11 02:00:00"
     */
    public static String formatAsUtcString(Instant instant) {
        if (instant == null) {
            return null;
        }
        ZonedDateTime utcTime = instant.atZone(ZONE_UTC);
        return utcTime.format(STANDARD_FORMATTER);
    }

    /**
     * 同时格式化为 UTC 和上海时间（用于日志）
     * 
     * <p>输出格式：timestamp (UTC: yyyy-MM-dd HH:mm:ss, CST: yyyy-MM-dd HH:mm:ss)
     * 
     * @param instant UTC 时间点
     * @return 格式化的字符串，包含时间戳、UTC时间和CST时间
     */
    public static String formatWithBothTimezones(Instant instant) {
        if (instant == null) {
            return "null";
        }
        String utcStr = formatAsUtcString(instant);
        String cstStr = formatAsShanghaiString(instant);
        return String.format("%d (UTC: %s, CST: %s)", instant.toEpochMilli(), utcStr, cstStr);
    }

    // ==================== 解析方法 ====================

    /**
     * 将上海时间字符串解析为 UTC Instant
     * 
     * <p>用于从前端接收用户输入的本地时间。
     * 输入格式：yyyy-MM-dd HH:mm:ss
     * 
     * @param shanghaiTimeStr 上海时区的时间字符串，例如 "2026-01-11 10:00:00"
     * @return 对应的 UTC Instant
     * @throws DateTimeParseException 如果字符串格式不正确
     * @throws IllegalArgumentException 如果字符串为空
     */
    public static Instant parseFromShanghaiString(String shanghaiTimeStr) {
        if (shanghaiTimeStr == null || shanghaiTimeStr.trim().isEmpty()) {
            throw new IllegalArgumentException("时间字符串不能为空");
        }
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(shanghaiTimeStr.trim(), STANDARD_FORMATTER);
            ZonedDateTime shanghaiTime = localDateTime.atZone(ZONE_SHANGHAI);
            return shanghaiTime.toInstant();
        } catch (DateTimeParseException e) {
            throw new DateTimeParseException(
                "时间格式错误，期望格式: yyyy-MM-dd HH:mm:ss，实际输入: " + shanghaiTimeStr,
                shanghaiTimeStr,
                e.getErrorIndex()
            );
        }
    }

    /**
     * 将 UTC 时间字符串解析为 Instant
     * 
     * <p>用于从配置文件或外部系统接收 UTC 时间字符串。
     * 输入格式：yyyy-MM-dd HH:mm:ss
     * 
     * @param utcTimeStr UTC 时间字符串，例如 "2026-01-11 02:00:00"
     * @return 对应的 Instant
     * @throws DateTimeParseException 如果字符串格式不正确
     * @throws IllegalArgumentException 如果字符串为空
     */
    public static Instant parseFromUtcString(String utcTimeStr) {
        if (utcTimeStr == null || utcTimeStr.trim().isEmpty()) {
            throw new IllegalArgumentException("时间字符串不能为空");
        }
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(utcTimeStr.trim(), STANDARD_FORMATTER);
            ZonedDateTime utcTime = localDateTime.atZone(ZONE_UTC);
            return utcTime.toInstant();
        } catch (DateTimeParseException e) {
            throw new DateTimeParseException(
                "时间格式错误，期望格式: yyyy-MM-dd HH:mm:ss，实际输入: " + utcTimeStr,
                utcTimeStr,
                e.getErrorIndex()
            );
        }
    }

    // ==================== 时间对齐方法 ====================

    /**
     * 将时间戳对齐到分钟起始点（向下取整）
     * 
     * <p>用于 K 线数据的时间对齐。
     * 例如：2026-01-11 10:00:35.123 → 2026-01-11 10:00:00.000
     * 
     * @param instant 原始时间点
     * @return 对齐到分钟起始点的时间点
     */
    public static Instant alignToMinuteStart(Instant instant) {
        if (instant == null) {
            throw new NullPointerException("Instant 不能为 null");
        }
        long epochMilli = instant.toEpochMilli();
        long alignedMilli = (epochMilli / 60000) * 60000;
        return Instant.ofEpochMilli(alignedMilli);
    }

    /**
     * 将时间戳对齐到小时起始点（向下取整）
     * 
     * @param instant 原始时间点
     * @return 对齐到小时起始点的时间点
     */
    public static Instant alignToHourStart(Instant instant) {
        if (instant == null) {
            throw new NullPointerException("Instant 不能为 null");
        }
        long epochMilli = instant.toEpochMilli();
        long alignedMilli = (epochMilli / 3600000) * 3600000;
        return Instant.ofEpochMilli(alignedMilli);
    }

    // ==================== 验证方法 ====================

    /**
     * 验证时间范围的有效性
     * 
     * @param start 开始时间
     * @param end 结束时间
     * @return true 表示有效（start < end），false 表示无效
     */
    public static boolean isValidTimeRange(Instant start, Instant end) {
        if (start == null || end == null) {
            return false;
        }
        return start.isBefore(end);
    }

    // ==================== 高级工具方法 ====================

    /**
     * 时间范围对象（用于封装查询参数）
     */
    public static class TimeRange {
        private final Instant start;
        private final Instant end;

        private TimeRange(Instant start, Instant end) {
            if (!isValidTimeRange(start, end)) {
                throw new IllegalArgumentException(
                    String.format("无效的时间范围: start=%s, end=%s", start, end)
                );
            }
            this.start = start;
            this.end = end;
        }

        public static TimeRange of(Instant start, Instant end) {
            return new TimeRange(start, end);
        }

        public Instant getStart() {
            return start;
        }

        public Instant getEnd() {
            return end;
        }

        @Override
        public String toString() {
            return String.format("TimeRange[%s, %s)", 
                formatAsUtcString(start), 
                formatAsUtcString(end)
            );
        }
    }

    /**
     * 从上海时间字符串创建时间范围
     * 
     * @param startShanghaiStr 开始时间（上海时区）
     * @param endShanghaiStr 结束时间（上海时区）
     * @return 时间范围对象
     */
    public static TimeRange createRangeFromShanghaiStrings(String startShanghaiStr, String endShanghaiStr) {
        Instant start = parseFromShanghaiString(startShanghaiStr);
        Instant end = parseFromShanghaiString(endShanghaiStr);
        return TimeRange.of(start, end);
    }
}
