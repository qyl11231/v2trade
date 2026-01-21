package com.qyl.v2trade.market.web.query.impl;

import com.qyl.v2trade.common.util.TimeUtil;
import com.qyl.v2trade.market.model.NormalizedKline;
import com.qyl.v2trade.market.web.query.MarketQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

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
                                             Instant fromTime, Instant toTime, Integer limit) {
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
            // 重构：按照时间管理约定，在数据库查询边界将 Instant 转换为 Timestamp
            // 重要：QuestDB 需要使用 CAST 将 epoch milliseconds 转换为 TIMESTAMP
            // 或者直接使用 Timestamp 类型，但需要确保 JDBC URL 配置了 timezone=UTC
            if (fromTime != null) {
                // 使用 Timestamp 类型，确保 JDBC 驱动正确处理时区
                // 注意：即使使用 CAST，QuestDB 也可能不支持，所以改回使用 Timestamp
                // 但需要确保 JDBC URL 配置了 timezone=UTC
                sql.append("AND ts >= ? ");
                Timestamp fromTimestamp = Timestamp.from(fromTime);
                params.add(fromTimestamp);
                long fromEpochMillis = TimeUtil.toEpochMilli(fromTime);
            }
            if (toTime != null) {
                // 使用 < 而不是 <=，确保左闭右开区间
                sql.append("AND ts < ? ");
                Timestamp toTimestamp = Timestamp.from(toTime);
                params.add(toTimestamp);
                long toEpochMillis = TimeUtil.toEpochMilli(toTime);
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
                if (fromTime != null) {
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
            // 记录完整的 SQL 语句用于调试
            String finalSql = sql.toString();
            List<NormalizedKline> klines = questDbJdbcTemplate.query(
                finalSql,
                params.toArray(),
                (rs, rowNum) -> mapRowToKline(rs, rowNum, interval)
            );

            // 如果是倒序查询（使用了limit且没有from），需要反转顺序为升序
            // 如果有from，已经是升序，不需要反转
            if (limit != null && limit > 0 && fromTime == null && !klines.isEmpty()) {
                java.util.Collections.reverse(klines);
            }

            log.debug("查询结果: symbol={}, interval={}, count={}, 排序=升序(旧->新)",
                     symbol, interval, klines.size());
            if (!klines.isEmpty()) {
                NormalizedKline first = klines.get(0);
                NormalizedKline last = klines.get(klines.size() - 1);
                log.debug("查询结果时间范围: 第一条={} (epochMillis={}), 最后一条={} (epochMillis={})",
                        first.getTimestampInstant() != null ? first.getTimestampInstant() + " (UTC)" : "null",
                        first.getTimestamp(),
                        last.getTimestampInstant() != null ? last.getTimestampInstant() + " (UTC)" : "null",
                        last.getTimestamp());
            }
            return klines;

        } catch (CannotGetJdbcConnectionException e) {
            // QuestDB连接失败，通常是QuestDB未启动或不可用
            // 使用WARN级别并减少日志频率，避免日志爆炸
            log.warn("QuestDB连接失败，无法查询K线: symbol={}, interval={}. 请确认QuestDB是否已启动 ({}:{})", 
                    symbol, interval, e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            return new ArrayList<>();
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
        } catch (CannotGetJdbcConnectionException e) {
            log.warn("QuestDB连接失败，无法查询最新K线: symbol={}, interval={}. 请确认QuestDB是否已启动", 
                    symbol, interval);
            return null;
        } catch (Exception e) {
            log.error("查询最新K线失败: symbol={}, interval={}", symbol, interval, e);
            return null;
        }
    }

    @Override
    public NormalizedKline queryKlineByTimestamp(String symbol, String interval, Instant timestamp) {
        try {
            // 重构：按照时间管理约定，在数据库查询边界将 Instant 转换为 Timestamp
            Timestamp ts = Timestamp.from(timestamp);
            
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
        } catch (CannotGetJdbcConnectionException e) {
            log.warn("QuestDB连接失败，无法查询指定时间K线: symbol={}, timestamp={}. 请确认QuestDB是否已启动", 
                    symbol, timestamp);
            return null;
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
     * 
     * 重要：使用 Calendar.getInstance(TimeZone.getTimeZone("UTC")) 明确指定UTC时区
     * 避免 rs.getTimestamp() 受 JVM 默认时区影响
     */
    private static final Calendar UTC_CALENDAR = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    
    private NormalizedKline mapRowToKline(ResultSet rs, int rowNum, String interval) throws SQLException {
        // 使用 UTC Calendar 明确指定时区，避免受 JVM 默认时区影响
        Timestamp ts = rs.getTimestamp("ts", UTC_CALENDAR);
        // QuestDB TIMESTAMP是UTC时间，直接转换为epoch millis
        long timestampMillis = ts != null ? ts.toInstant().toEpochMilli() : 0;
        
        NormalizedKline kline = NormalizedKline.builder()
                .symbol(rs.getString("symbol"))
                .interval(interval)
                .open(rs.getDouble("open"))
                .high(rs.getDouble("high"))
                .low(rs.getDouble("low"))
                .close(rs.getDouble("close"))
                .volume(rs.getDouble("volume"))
                .build();
        // 使用兼容性方法设置时间戳（long -> Instant）
        kline.setTimestamp(timestampMillis);
        kline.setExchangeTimestamp(rs.getLong("exchange_ts"));
        
        // 调试日志：记录查询结果的时间戳（仅第一条）
        if (rowNum == 0) {
            Instant tsInstant = ts != null ? ts.toInstant() : null;
            log.debug("查询结果第一条K线: Timestamp={}, epochMillis={}, Instant={} (UTC), 上海时间={}",
                    ts, timestampMillis, tsInstant, 
                    tsInstant != null ? TimeUtil.formatAsShanghaiString(tsInstant) : "null");
        }
        
        return kline;
    }
}

