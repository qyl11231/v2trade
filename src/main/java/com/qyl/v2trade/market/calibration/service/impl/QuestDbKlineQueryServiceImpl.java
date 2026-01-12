package com.qyl.v2trade.market.calibration.service.impl;

import com.qyl.v2trade.common.util.TimeUtil;
import com.qyl.v2trade.market.calibration.service.QuestDbKlineQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

/**
 * QuestDB K线查询服务实现类
 */
@Slf4j
@Service
public class QuestDbKlineQueryServiceImpl implements QuestDbKlineQueryService {

    @Autowired
    @Qualifier("questDbJdbcTemplate")
    private JdbcTemplate questDbJdbcTemplate;


    private static final Calendar UTC_CALENDAR = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    /**
     * 查询指定时间范围内已存在的K线时间戳列表
     * 
     * <p>时间语义：所有时间都是UTC Instant。
     * 时间边界：左闭右开区间 [startTime, endTime)
     *
     * <p>注意：QuestDB TIMESTAMP字段语义为UTC，查询时使用 >= start AND < end。
     */
    @Override
    public List<Long> queryExistingTimestamps(String symbol, Instant startTime, Instant endTime) {
        try {
            // QuestDB查询SQL
            // 时间边界规范：ts >= start AND ts < end (左闭右开区间)
            // QuestDB TIMESTAMP是UTC时间，使用 Timestamp.from(instant) 转换
            String sql = "SELECT ts FROM kline_1m " +
                        "WHERE symbol = ? " +
                        "AND ts >= ? AND ts < ? " +
                        "ORDER BY ts ASC";

            Timestamp startTs = Timestamp.from(startTime);
            Timestamp endTs = Timestamp.from(endTime);

            // 查询所有时间戳
            // QuestDB TIMESTAMP是UTC，转换为epoch millis (UTC)
            // 对齐到分钟起始点：K线时间戳必须对齐到分钟起始点（去掉秒和毫秒部分）
            List<Long> allTimestamps = questDbJdbcTemplate.query(
                sql,
                (rs, rowNum) -> {
                    Timestamp ts = rs.getTimestamp("ts", UTC_CALENDAR);
                    if (ts == null) {
                        return null;
                    }
                    long timestampMillis = ts.toInstant().toEpochMilli();
                    // 对齐到分钟起始点（向下取整）
                    return (timestampMillis / 60000) * 60000;
                },
                symbol, startTs, endTs
            );

            // 转换为排序列表（去重）
            Set<Long> uniqueSet = new HashSet<>(allTimestamps);
            List<Long> result = new ArrayList<>(uniqueSet);
            result.sort(Long::compareTo);

            log.debug("查询已存在的K线时间戳完成: symbol={}, 总数={}, 去重后分钟数={}, 时间范围: {} ~ {}", 
                    symbol, allTimestamps.size(), result.size(),
                    TimeUtil.formatWithBothTimezones(startTime),
                    TimeUtil.formatWithBothTimezones(endTime));

            return result;
        } catch (Exception e) {
            log.error("查询已存在的K线时间戳失败: symbol={}, startTime={}, endTime={}", 
                    symbol, 
                    startTime != null ? TimeUtil.formatWithBothTimezones(startTime) : "null",
                    endTime != null ? TimeUtil.formatWithBothTimezones(endTime) : "null", e);
            return new ArrayList<>();
        }
    }
}

