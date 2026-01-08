package com.qyl.v2trade.indicator.series;

import com.qyl.v2trade.business.system.model.entity.ExchangeMarketPair;
import com.qyl.v2trade.business.system.service.ExchangeMarketPairService;
import com.qyl.v2trade.indicator.domain.model.NormalizedBar;
import com.qyl.v2trade.indicator.infrastructure.resolver.TradingPairResolver;
import com.qyl.v2trade.indicator.infrastructure.time.QuestDbAdapter;
import com.qyl.v2trade.market.model.NormalizedKline;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * QuestDB K线读取器
 * 
 * <p>从QuestDB聚合表读取K线数据，转换为NormalizedBar
 * 
 * <p>规则：
 * - 只读聚合表（kline_{period}）
 * - bar_time必须是bar_close_time（UTC）
 * - 使用TimeAlignmentAdapter统一归一化
 *
 * @author qyl
 */
@Slf4j
@Component
public class QuestDbKlineReader {
    
    @Autowired
    @Qualifier("questDbJdbcTemplate")
    private JdbcTemplate questDbJdbcTemplate;
    
    @Autowired(required = false)
    private TradingPairResolver tradingPairResolver;
    
    @Autowired
    private QuestDbAdapter questDbAdapter;
    
    @Autowired(required = false)
    private ExchangeMarketPairService exchangeMarketPairService;
    
    /**
     * 默认交易所代码（当前只支持OKX）
     */
    private static final String DEFAULT_EXCHANGE_CODE = "OKX";
    
    /**
     * 加载指定交易对和周期的最新N根K线
     * 
     * <p>只支持聚合周期（5m、15m、30m、1h、4h），不支持1m
     * 
     * @param pairId 交易对ID
     * @param timeframe 周期（如：5m, 15m, 1h），必须为支持的周期
     * @param limit 数量（最多365根）
     * @return K线列表，按时间升序，barTime已归一化为bar_close_time
     */
    public List<NormalizedBar> loadLatestBars(long pairId, String timeframe, int limit) {
        // 0. 验证周期是否支持
        if (!com.qyl.v2trade.indicator.infrastructure.time.QuestDbTsSemanticsProbe
                .isTimeframeSupported(timeframe)) {
            log.warn("不支持的周期: pairId={}, timeframe={} (指标模块只支持5m/15m/30m/1h/4h)", 
                    pairId, timeframe);
            return new ArrayList<>();
        }
        
        // 1. 获取symbolOnExchange（QuestDB中存储的是交易所格式的symbol，如BTC-USDT-SWAP）
        String symbolOnExchange = null;
        if (exchangeMarketPairService != null) {
            try {
                ExchangeMarketPair exchangePair = exchangeMarketPairService.getByExchangeAndTradingPairId(
                        DEFAULT_EXCHANGE_CODE, pairId);
                if (exchangePair != null) {
                    String exchangeSymbol = exchangePair.getSymbolOnExchange();
                    if (exchangeSymbol != null && !exchangeSymbol.isEmpty()) {
                        symbolOnExchange = exchangeSymbol;
                    }
                }
            } catch (Exception e) {
                log.warn("查询ExchangeMarketPair失败: pairId={}, exchangeCode={}", 
                        pairId, DEFAULT_EXCHANGE_CODE, e);
            }
        }
        
        // 降级方案：如果无法获取symbolOnExchange，尝试使用标准symbol
        if (symbolOnExchange == null && tradingPairResolver != null) {
            String standardSymbol = tradingPairResolver.tradingPairIdToSymbol(pairId);
            if (standardSymbol != null) {
                // 尝试通过marketType推断symbolOnExchange
                // 但这里无法获取marketType，所以只能使用标准symbol
                symbolOnExchange = standardSymbol;
                log.warn("无法获取symbolOnExchange，使用标准symbol: pairId={}, symbol={} (可能查询失败)", 
                        pairId, standardSymbol);
            }
        }
        
        if (symbolOnExchange == null || symbolOnExchange.isEmpty()) {
            log.warn("无法获取symbol: pairId={} (ExchangeMarketPairService={}, TradingPairResolver={})", 
                    pairId, 
                    exchangeMarketPairService != null ? "已注入" : "未注入",
                    tradingPairResolver != null ? "已注入" : "未注入");
            return new ArrayList<>();
        }
        
        log.debug("准备查询QuestDB: pairId={}, symbolOnExchange={}, timeframe={}, table=kline_{}", 
                pairId, symbolOnExchange, timeframe, timeframe);
        
        // 2. 查询QuestDB聚合表（不查询1m表）
        String tableName = "kline_" + timeframe;
        String sql = String.format(
            "SELECT symbol, ts, open, high, low, close, volume " +
            "FROM %s " +
            "WHERE symbol = ? " +
            "ORDER BY ts DESC " +
            "LIMIT ?",
            tableName
        );
        
        try {
            // 3. 查询原始K线数据（使用symbolOnExchange）
            int limitValue = Math.min(limit, 365);
            List<NormalizedKline> rawKlines = questDbJdbcTemplate.query(
                sql,
                (RowMapper<NormalizedKline>) (rs, rowNum) -> mapRowToNormalizedKline(rs, timeframe),
                symbolOnExchange, limitValue
            );
            
            // 4. 反转顺序（从旧到新）
            Collections.reverse(rawKlines);
            
            // 5. 使用TimeAlignmentAdapter归一化为NormalizedBar
            List<NormalizedBar> normalizedBars = new ArrayList<>();
            for (NormalizedKline rawKline : rawKlines) {
                try {
                    NormalizedBar bar = questDbAdapter.normalize(rawKline);
                    normalizedBars.add(bar);
                } catch (Exception e) {
                    log.warn("归一化K线失败: symbol={}, timeframe={}, timestamp={}",
                            rawKline.getSymbol(), rawKline.getInterval(), rawKline.getTimestamp(), e);
                }
            }
            
            log.debug("加载K线: pairId={}, symbolOnExchange={}, timeframe={}, limit={}, loaded={}",
                    pairId, symbolOnExchange, timeframe, limit, normalizedBars.size());
            
            // 6. 验证时间步长
            validateTimeStep(normalizedBars, timeframe);
            
            return normalizedBars;
            
        } catch (Exception e) {
            log.error("查询QuestDB K线失败: pairId={}, symbolOnExchange={}, timeframe={}",
                    pairId, symbolOnExchange, timeframe, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 将ResultSet映射为NormalizedKline
     */
    private NormalizedKline mapRowToNormalizedKline(ResultSet rs, String timeframe) throws SQLException {
        Timestamp ts = rs.getTimestamp("ts");
        long timestampMillis = ts != null ? ts.toInstant().toEpochMilli() : 0;
        
        return NormalizedKline.builder()
                .symbol(rs.getString("symbol"))
                .interval(timeframe)
                .open(rs.getDouble("open"))
                .high(rs.getDouble("high"))
                .low(rs.getDouble("low"))
                .close(rs.getDouble("close"))
                .volume(rs.getDouble("volume"))
                .timestamp(timestampMillis)
                .build();
    }
    
    /**
     * 验证时间步长是否严格等于timeframe duration
     */
    private void validateTimeStep(List<NormalizedBar> bars, String timeframe) {
        if (bars.size() < 2) {
            return;
        }
        
        long expectedDuration = getTimeframeDurationSeconds(timeframe);
        
        for (int i = 1; i < bars.size(); i++) {
            java.time.Duration duration = java.time.Duration.between(
                bars.get(i - 1).barTime(),
                bars.get(i).barTime()
            );
            long actualSeconds = duration.getSeconds();
            
            if (actualSeconds != expectedDuration) {
                log.warn("时间步长异常: timeframe={}, expected={}s, actual={}s, " +
                                "bar1={}, bar2={}",
                        timeframe, expectedDuration, actualSeconds,
                        bars.get(i - 1).barTime(), bars.get(i).barTime());
            }
        }
    }
    
    /**
     * 获取周期对应的秒数
     */
    private long getTimeframeDurationSeconds(String timeframe) {
        if (timeframe == null || timeframe.isEmpty()) {
            return 60; // 默认1分钟
        }
        
        try {
            String numberStr = timeframe.replaceAll("[^0-9]", "");
            String unit = timeframe.replaceAll("[0-9]", "").toLowerCase();
            
            if (numberStr.isEmpty()) {
                return 60;
            }
            
            long number = Long.parseLong(numberStr);
            
            switch (unit) {
                case "m":
                    return number * 60;
                case "h":
                    return number * 3600;
                case "d":
                    return number * 86400;
                case "w":
                    return number * 604800;
                default:
                    return 60;
            }
        } catch (Exception e) {
            return 60;
        }
    }
}

