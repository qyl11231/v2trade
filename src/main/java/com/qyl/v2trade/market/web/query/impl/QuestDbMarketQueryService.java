package com.qyl.v2trade.market.web.query.impl;

import com.qyl.v2trade.market.model.NormalizedKline;
import com.qyl.v2trade.market.web.query.MarketQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * QuestDB行情查询服务实现
 */
@Slf4j
@Service("questDbMarketQueryService")
public class QuestDbMarketQueryService implements MarketQueryService {

    @Autowired
    @Qualifier("questDbJdbcTemplate")
    private JdbcTemplate questDbJdbcTemplate;

    private static final int DEFAULT_LIMIT = 1000;
    private static final int MAX_LIMIT = 10000;

    @Override
    public List<NormalizedKline> queryKlines(String symbol, String interval, 
                                             Long fromTimestamp, Long toTimestamp, Integer limit) {
        try {
            // 根据周期选择表名
            String tableName = getTableName(interval);
            
            // 构建SQL查询
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT symbol, ts, open, high, low, close, volume, exchange_ts ");
            sql.append("FROM ").append(tableName).append(" ");
            sql.append("WHERE symbol = ? ");

            List<Object> params = new ArrayList<>();
            params.add(symbol);

            // 添加时间范围条件
            // 时间边界规范：ts >= start AND ts < end (左闭右开区间，避免边界重复)
            // 时间戳语义：epoch millis (UTC)，转换为Instant后查询QuestDB TIMESTAMP (UTC)
            if (fromTimestamp != null) {
                sql.append("AND ts >= ? ");
                Instant fromInstant = Instant.ofEpochMilli(fromTimestamp);
                params.add(Timestamp.from(fromInstant));
            }
            if (toTimestamp != null) {
                // 使用 < 而不是 <=，确保左闭右开区间
                sql.append("AND ts < ? ");
                Instant toInstant = Instant.ofEpochMilli(toTimestamp);
                params.add(Timestamp.from(toInstant));
            }

            // ========== 系统级重构：正确的K线数据查询逻辑 ==========
            // 核心原则：
            // 1. 始终返回升序数据（从旧到新），符合TradingView图表库要求
            // 2. 如果指定limit，有两种场景：
            //    a) 如果没有指定from/to，返回最新的N条（按倒序取，然后反转）
            //    b) 如果指定了from/to，返回从from开始的连续N条（按升序取）
            // 3. 历史数据加载场景：需要从from开始的连续N条，而不是时间范围内最新的N条
            
            if (limit != null && limit > 0) {
                int limitValue = Math.min(limit, MAX_LIMIT);
                
                // 如果指定了from，说明是历史数据加载场景，需要返回从from开始的连续N条
                // 使用升序查询，确保返回从from开始的连续数据
                if (fromTimestamp != null) {
                    sql.append("ORDER BY ts ASC ");
                    sql.append("LIMIT ?");
                    params.add(limitValue);
                } else {
                    // 没有from，返回最新的N条：先按倒序取，然后反转
                    sql.append("ORDER BY ts DESC ");
                    sql.append("LIMIT ?");
                    params.add(limitValue);
                }
            } else {
                // 无limit限制，直接按升序返回所有数据
                sql.append("ORDER BY ts ASC ");
            }

            // 执行查询
            List<NormalizedKline> klines = questDbJdbcTemplate.query(
                sql.toString(),
                params.toArray(),
                (rs, rowNum) -> mapRowToKline(rs, rowNum, interval)
            );

            // 如果是倒序查询（使用了limit且没有from），需要反转顺序为升序
            // 如果有from，已经是升序，不需要反转
            if (limit != null && limit > 0 && fromTimestamp == null && !klines.isEmpty()) {
                java.util.Collections.reverse(klines);
            }

            log.debug("查询K线: symbol={}, interval={}, count={}, 排序=升序(旧->新)", 
                     symbol, interval, klines.size());
            return klines;

        } catch (Exception e) {
            log.error("查询K线失败: symbol={}, interval={}", symbol, interval, e);
            return new ArrayList<>();
        }
    }

    @Override
    public NormalizedKline queryLatestKline(String symbol, String interval) {
        try {
            String tableName = getTableName(interval);
            String sql = "SELECT symbol, ts, open, high, low, close, volume, exchange_ts " +
                         "FROM " + tableName + " " +
                         "WHERE symbol = ? " +
                         "ORDER BY ts DESC " +
                         "LIMIT 1";

            List<NormalizedKline> klines = questDbJdbcTemplate.query(
                sql,
                new Object[]{symbol},
                (rs, rowNum) -> mapRowToKline(rs, rowNum, interval)
            );

            return klines.isEmpty() ? null : klines.get(0);
        } catch (Exception e) {
            log.error("查询最新K线失败: symbol={}, interval={}", symbol, interval, e);
            return null;
        }
    }

    @Override
    public NormalizedKline queryKlineByTimestamp(String symbol, String interval, long timestamp) {
        try {
            // 时间戳语义：epoch millis (UTC)，转换为Instant后查询QuestDB TIMESTAMP (UTC)
            Instant timestampInstant = Instant.ofEpochMilli(timestamp);
            Timestamp ts = Timestamp.from(timestampInstant);
            
            String tableName = getTableName(interval);
            String sql = "SELECT symbol, ts, open, high, low, close, volume, exchange_ts " +
                         "FROM " + tableName + " " +
                         "WHERE symbol = ? AND ts = ? " +
                         "LIMIT 1";

            List<NormalizedKline> klines = questDbJdbcTemplate.query(
                sql,
                new Object[]{symbol, ts},
                (rs, rowNum) -> mapRowToKline(rs, rowNum, interval)
            );

            return klines.isEmpty() ? null : klines.get(0);
        } catch (Exception e) {
            log.error("查询指定时间K线失败: symbol={}, timestamp={}", symbol, timestamp, e);
            return null;
        }
    }

    /**
     * 根据周期获取表名
     */
    private String getTableName(String interval) {
        return "kline_" + interval;
    }

    /**
     * 将ResultSet行映射为NormalizedKline
     * 
     * QuestDB TIMESTAMP字段语义为UTC，转换为epoch millis (UTC)
     */
    private NormalizedKline mapRowToKline(ResultSet rs, int rowNum, String interval) throws SQLException {
        Timestamp ts = rs.getTimestamp("ts");
        // QuestDB TIMESTAMP是UTC时间，直接转换为epoch millis
        long timestampMillis = ts != null ? ts.toInstant().toEpochMilli() : 0;
        
        return NormalizedKline.builder()
                .symbol(rs.getString("symbol"))
                .interval(interval)
                .open(rs.getDouble("open"))
                .high(rs.getDouble("high"))
                .low(rs.getDouble("low"))
                .close(rs.getDouble("close"))
                .volume(rs.getDouble("volume"))
                .timestamp(timestampMillis)
                .exchangeTimestamp(rs.getLong("exchange_ts"))
                .build();
    }
}

