package com.qyl.v2trade.market.calibration.service.impl;

import com.qyl.v2trade.market.calibration.service.QuestDbKlineQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * QuestDB K线查询服务实现类
 */
@Slf4j
@Service
public class QuestDbKlineQueryServiceImpl implements QuestDbKlineQueryService {

    @Autowired
    @Qualifier("questDbJdbcTemplate")
    private JdbcTemplate questDbJdbcTemplate;

    /**
     * 查询指定时间范围内已存在的K线时间戳列表
     * 
     * <p>时间语义：所有时间戳都是UTC epoch millis。
     * 时间边界：左闭右开区间 [startTimestamp, endTimestamp)
     *
     * <p>注意：QuestDB TIMESTAMP字段语义为UTC，查询时使用 >= start AND < end。
     */
    @Override
    public List<Long> queryExistingTimestamps(String symbol, long startTimestamp, long endTimestamp) {
        // 日志同时打印UTC和本地时间
        Instant startInstant = Instant.ofEpochMilli(startTimestamp);
        Instant endInstant = Instant.ofEpochMilli(endTimestamp);


        try {
            // QuestDB查询SQL
            // 时间边界规范：ts >= start AND ts < end (左闭右开区间)
            // QuestDB TIMESTAMP是UTC时间，直接使用Instant转换
            String sql = "SELECT ts FROM kline_1m " +
                        "WHERE symbol = ? " +
                        "AND ts >= ? AND ts < ? " +
                        "ORDER BY ts ASC";

            Timestamp startTs = Timestamp.from(startInstant);
            Timestamp endTs = Timestamp.from(endInstant);

            // 查询所有时间戳
            // QuestDB TIMESTAMP是UTC，转换为epoch millis (UTC)
            // 对齐到分钟起始点：K线时间戳必须对齐到分钟起始点（去掉秒和毫秒部分）
            List<Long> allTimestamps = questDbJdbcTemplate.query(
                sql,
                (rs, rowNum) -> {
                    Timestamp ts = rs.getTimestamp("ts");
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

            log.debug("查询已存在的K线时间戳完成: symbol={}, 总数={}, 去重后分钟数={}", 
                    symbol, allTimestamps.size(), result.size());

            return result;
        } catch (Exception e) {
            log.error("查询已存在的K线时间戳失败: symbol={}, startTimestamp={}, endTimestamp={}", 
                    symbol, startTimestamp, endTimestamp, e);
            return new ArrayList<>();
        }
    }
}

