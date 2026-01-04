package com.qyl.v2trade.market.subscription.persistence.storage.impl;

import com.qyl.v2trade.market.model.NormalizedKline;
import com.qyl.v2trade.market.subscription.persistence.storage.MarketStorageService;
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
import java.util.List;

/**
 * QuestDB行情存储服务实现
 */
@Slf4j
@Service
public class QuestDbMarketStorageService implements MarketStorageService {

    @Autowired
    @Qualifier("questDbJdbcTemplate")
    private JdbcTemplate questDbJdbcTemplate;

    /**
     * 同步锁（用于防止同一时间戳的并发插入）
     * 使用 ConcurrentHashMap 的 key 作为锁对象
     */
    private final java.util.concurrent.ConcurrentHashMap<String, Object> locks = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * QuestDB INSERT 语句
     * 
     * <p>注意：QuestDB 不支持标准的 DELETE 和 UPSERT 语法。
     * 对于重复数据，使用应用层去重逻辑（在 MarketDataCenter 中处理）。
     * 
     * <p>QuestDB 作为时序数据库，允许同一时间戳有多条数据（用于处理K线更新场景）。
     * 查询时可以使用 LATEST BY 或 ASOF JOIN 获取最新数据。
     */
    private static final String INSERT_SQL = 
        "INSERT INTO kline_1m (symbol, ts, open, high, low, close, volume, exchange_ts) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String EXISTS_SQL = 
        "SELECT COUNT(*) FROM kline_1m WHERE symbol = ? AND ts = ?";

    @Override
    public boolean saveKline(NormalizedKline kline) {
        // 使用同步锁防止同一时间戳的并发插入
        String lockKey = kline.getSymbol() + ":" + kline.getTimestamp();
        Object lock = locks.computeIfAbsent(lockKey, k -> new Object());

        synchronized (lock) {
            try {
                // 双重检查：检查是否已存在相同时间戳的数据（不管数据是否相同）
                // 保证同一时间戳只保留一条数据，避免重复
                // 注意：即使应用层已经去重，这里也要检查（双重保险）
                if (exists(kline.getSymbol(), kline.getTimestamp())) {
                    log.warn("K线已存在（相同时间戳），跳过: symbol={}, timestamp={}",
                            kline.getSymbol(), kline.getTimestamp());
                    return false;
                }

                // 插入新的K线数据
                // 时间戳语义：epoch millis (UTC)，转换为Instant后写入QuestDB TIMESTAMP (UTC)
                Instant timestampInstant = Instant.ofEpochMilli(kline.getTimestamp());
                Timestamp timestamp = Timestamp.from(timestampInstant);

                long exchangeTs = kline.getExchangeTimestamp() != null ? kline.getExchangeTimestamp() : kline.getTimestamp();

                int rows = questDbJdbcTemplate.update(INSERT_SQL,
                    kline.getSymbol(),
                    timestamp,
                    kline.getOpen(),
                    kline.getHigh(),
                    kline.getLow(),
                    kline.getClose(),
                    kline.getVolume(),
                    exchangeTs
                );

                if (rows > 0) {
                    // 日志同时打印UTC和本地时间（Asia/Shanghai）
                    ZonedDateTime utcTime = timestampInstant.atZone(ZoneId.of("UTC"));
                    ZonedDateTime localTime = timestampInstant.atZone(ZoneId.of("Asia/Shanghai"));
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    log.debug("K线已保存到QuestDB: symbol={}, timestamp={} (UTC: {}, CST: {}), open={}, high={}, low={}, close={}, volume={}",
                            kline.getSymbol(), kline.getTimestamp(),
                            utcTime.format(formatter), localTime.format(formatter),
                            kline.getOpen(), kline.getHigh(), kline.getLow(), kline.getClose(), kline.getVolume());
                } else {
                    log.warn("K线保存失败（返回0行）: symbol={}, timestamp={}",
                            kline.getSymbol(), kline.getTimestamp());
                }

                return rows > 0;
            } catch (Exception e) {
                log.error("保存K线失败: symbol={}, timestamp={}", kline.getSymbol(), kline.getTimestamp(), e);
                return false;
            }
        }
    }

    @Override
    public int batchSaveKlines(List<NormalizedKline> klines) {
        if (klines == null || klines.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (NormalizedKline kline : klines) {
            if (saveKline(kline)) {
                successCount++;
            }
        }

        log.debug("批量保存K线: 总数={}, 成功={}", klines.size(), successCount);
        return successCount;
    }

    @Override
    public boolean exists(String symbol, long timestamp) {
        try {
            // 时间戳语义：epoch millis (UTC)，转换为Instant后写入QuestDB TIMESTAMP (UTC)
            Instant timestampInstant = Instant.ofEpochMilli(timestamp);
            Timestamp ts = Timestamp.from(timestampInstant);
            
            Integer count = questDbJdbcTemplate.queryForObject(
                EXISTS_SQL,
                Integer.class,
                symbol,
                ts
            );
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("检查K线是否存在失败: symbol={}, timestamp={}", symbol, timestamp, e);
            return false;
        }
    }
}

