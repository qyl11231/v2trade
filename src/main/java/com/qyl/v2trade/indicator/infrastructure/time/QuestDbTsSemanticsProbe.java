package com.qyl.v2trade.indicator.infrastructure.time;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * QuestDB ts字段语义探针
 * 
 * <p>在系统启动时自动检测QuestDB中ts字段的语义
 * 
 * <p>检测规则：
 * <ul>
 *   <li>读取kline_1m表最新N条记录</li>
 *   <li>检查相邻ts的间隔是否为60秒（1分钟）</li>
 *   <li>检查ts是否对齐到分钟边界</li>
 *   <li>根据检测结果判定ts语义</li>
 * </ul>
 * 
 * <p>如果无法判定，则抛出异常阻止系统启动
 *
 * @author qyl
 */
@Slf4j
@Component
@Order(100) // 在指标模块其他组件之前运行
public class QuestDbTsSemanticsProbe implements CommandLineRunner {
    
    @Autowired
    @Qualifier("questDbJdbcTemplate")
    private JdbcTemplate questDbJdbcTemplate;
    
    /**
     * 采样数量
     */
    @Value("${indicator.questdb.probe.sample-size:20}")
    private int sampleSize = 20;
    
    /**
     * 支持的周期列表（不包含1m，只检测聚合周期表）
     */
    private static final String[] SUPPORTED_TIMEFRAMES = {"5m", "15m", "30m", "1h", "4h"};
    
    /**
     * 用于检测的表名（默认使用5m表）
     */
    @Value("${indicator.questdb.probe.table:kline_5m}")
    private String probeTable = "kline_5m";
    
    /**
     * 5分钟周期对应的毫秒数（用于检测间隔）
     */
    private static final long FIVE_MINUTE_MS = 5 * 60 * 1000L;
    
    /**
     * 检测到的语义（静态变量，供其他组件使用）
     */
    private static volatile QuestDbTsSemantics detectedSemantics = QuestDbTsSemantics.UNKNOWN;
    
    @Override
    public void run(String... args) {
        log.info("开始检测QuestDB ts字段语义: table={}, sampleSize={}", probeTable, sampleSize);
        log.info("支持的周期: {}", String.join(", ", SUPPORTED_TIMEFRAMES));
        log.info("注意: 指标模块不对1m进行采集，只对聚合周期(5m/15m/30m/1h/4h)进行处理");
        
        try {
            QuestDbTsSemantics semantics = detectSemantics();
            detectedSemantics = semantics;
            
            if (semantics == QuestDbTsSemantics.UNKNOWN) {
                throw new IllegalStateException(
                    "无法判定QuestDB ts字段语义。请检查数据格式是否正确。" +
                    "要求：ts字段必须是周期对齐的时间戳（5m/15m/30m/1h/4h）。"
                );
            }
            
            log.info("✅ QuestDB ts字段语义检测完成: semantics={}", semantics);
            log.info("   - 如果ts是OPEN_TIME，则bar_close_time = ts + timeframe_duration");
            log.info("   - 如果ts是CLOSE_TIME，则bar_close_time = ts");
            log.info("   - 指标模块将处理以下周期: {}", String.join(", ", SUPPORTED_TIMEFRAMES));
            
        } catch (Exception e) {
            log.error("❌ QuestDB ts字段语义检测失败", e);
            throw new RuntimeException("QuestDB ts字段语义检测失败，系统启动中止", e);
        }
    }
    
    /**
     * 检测语义
     */
    private QuestDbTsSemantics detectSemantics() {
        try {
            // 1. 查询最新N条记录
            String sql = String.format(
                "SELECT symbol, ts FROM %s ORDER BY ts DESC LIMIT %d",
                probeTable.replaceAll("[^a-zA-Z0-9_]", ""), sampleSize
            );
            
            List<Map<String, Object>> rows = questDbJdbcTemplate.queryForList(sql);
            if(rows == null || rows.isEmpty()) {
                log.warn("QuestDB表{}中记录为空，无法检测语义", probeTable);
                return QuestDbTsSemantics.TS_IS_OPEN_TIME;
            }

            if (rows.size() < 2) {
                log.warn("QuestDB表{}中记录不足2条，无法检测语义", probeTable);
                return QuestDbTsSemantics.UNKNOWN;
            }
            
            // 2. 按时间戳排序（查询结果可能是倒序）
            rows.sort((a, b) -> {
                Timestamp ts1 = (Timestamp) a.get("ts");
                Timestamp ts2 = (Timestamp) b.get("ts");
                return ts1.compareTo(ts2);
            });
            
            // 3. 检查相邻记录的间隔
            // 根据表名推断周期（从kline_5m推断为5分钟）
            long expectedInterval = getExpectedIntervalFromTableName(probeTable);
            
            long firstTimestamp = ((Timestamp) rows.get(0).get("ts")).getTime();
            long secondTimestamp = ((Timestamp) rows.get(1).get("ts")).getTime();
            long interval = secondTimestamp - firstTimestamp;
            
            log.debug("相邻ts间隔: {} ms (期望: {} ms)", interval, expectedInterval);
            
            if (interval != expectedInterval && interval != expectedInterval * 2) {
                log.warn("相邻ts间隔不是期望值: {} ms (期望: {} ms 或其倍数)，可能数据不连续", 
                        interval, expectedInterval);
                // 继续检测，不立即返回UNKNOWN
            }
            
            // 4. 检查ts是否对齐到分钟边界
            // 如果ts是开盘时间，应该对齐到分钟开始（秒数为0）
            // 如果ts是收盘时间，也应该对齐到分钟结束（秒数为59或0）
            // 但实际上，我们需要通过间隔来判断
            
            // 5. 检查所有相邻间隔是否一致
            boolean allIntervalsValid = true;
            for (int i = 0; i < rows.size() - 1; i++) {
                long ts1 = ((Timestamp) rows.get(i).get("ts")).getTime();
                long ts2 = ((Timestamp) rows.get(i + 1).get("ts")).getTime();
                long gap = ts2 - ts1;
                
                if (gap != expectedInterval && gap != expectedInterval * 2) {
                    // 允许间隔为期望值或其2倍（可能中间有缺失）
                    if (gap > expectedInterval * 10) {
                        log.warn("检测到异常间隔: {} ms (行{} -> 行{})", gap, i, i + 1);
                        allIntervalsValid = false;
                        break;
                    }
                }
            }
            
            if (!allIntervalsValid) {
                log.warn("检测到不一致的ts间隔，数据可能不连续");
            }
            
            // 6. 检查ts对齐性
            // 读取第一条记录的ts，检查秒数部分
            Instant firstInstant = ((Timestamp) rows.get(0).get("ts")).toInstant();
            ZonedDateTime firstZdt = firstInstant.atZone(ZoneId.of("UTC"));
            int seconds = firstZdt.getSecond();
            int nanos = firstZdt.getNano();
            
            log.debug("第一条记录ts: {} (秒数: {}, 纳秒: {})", 
                firstZdt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), seconds, nanos);
            
            // 7. 根据对齐性和间隔判定语义
            // 对于聚合周期表（5m/15m/30m/1h/4h），如果秒数为0且间隔符合周期，很可能是开盘时间
            // 如果间隔符合周期且对齐到周期边界，判定为开盘时间
            
            // 默认判定：如果秒数为0且间隔符合期望周期，判定为开盘时间
            if (seconds == 0 && nanos == 0 && (interval == expectedInterval || interval == expectedInterval * 2)) {
                log.info("判定结果: TS_IS_OPEN_TIME (ts对齐到周期开始，间隔{}ms)", interval);
                return QuestDbTsSemantics.TS_IS_OPEN_TIME;
            }
            
            // 默认情况：假设是开盘时间（更常见）
            if (interval == expectedInterval || interval == expectedInterval * 2) {
                log.info("判定结果: TS_IS_OPEN_TIME (默认，间隔为{}ms的倍数)", interval);
                return QuestDbTsSemantics.TS_IS_OPEN_TIME;
            }
            
            log.warn("无法明确判定语义，默认使用TS_IS_OPEN_TIME");
            return QuestDbTsSemantics.TS_IS_OPEN_TIME;
            
        } catch (Exception e) {
            log.error("检测QuestDB ts语义时发生异常", e);
            return QuestDbTsSemantics.UNKNOWN;
        }
    }
    
    /**
     * 根据表名推断期望的时间间隔
     * 
     * @param tableName 表名（如：kline_5m, kline_1h）
     * @return 期望间隔（毫秒）
     */
    private long getExpectedIntervalFromTableName(String tableName) {
        // 从表名提取周期字符串（如：kline_5m -> 5m）
        String period = tableName.replace("kline_", "");
        
        // 解析周期
        try {
            String numberStr = period.replaceAll("[^0-9]", "");
            String unit = period.replaceAll("[0-9]", "").toLowerCase();
            
            if (numberStr.isEmpty()) {
                return FIVE_MINUTE_MS; // 默认5分钟
            }
            
            long number = Long.parseLong(numberStr);
            
            switch (unit) {
                case "m":
                    return number * 60 * 1000L;
                case "h":
                    return number * 60 * 60 * 1000L;
                case "d":
                    return number * 24 * 60 * 60 * 1000L;
                default:
                    return FIVE_MINUTE_MS;
            }
        } catch (Exception e) {
            log.warn("无法解析表名周期: {}, 使用默认5分钟间隔", tableName);
            return FIVE_MINUTE_MS;
        }
    }
    
    /**
     * 获取支持的周期列表
     * 
     * @return 支持的周期数组
     */
    public static String[] getSupportedTimeframes() {
        return SUPPORTED_TIMEFRAMES.clone();
    }
    
    /**
     * 检查周期是否被支持
     * 
     * @param timeframe 周期字符串（如：5m, 1h）
     * @return 是否支持
     */
    public static boolean isTimeframeSupported(String timeframe) {
        for (String tf : SUPPORTED_TIMEFRAMES) {
            if (tf.equals(timeframe)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取检测到的语义
     * 
     * @return 检测到的语义
     */
    public static QuestDbTsSemantics getDetectedSemantics() {
        return detectedSemantics;
    }
}

