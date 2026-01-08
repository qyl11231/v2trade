package com.qyl.v2trade.market.aggregation.web;

import com.qyl.v2trade.common.Result;
import com.qyl.v2trade.market.aggregation.config.SupportedPeriod;
import com.qyl.v2trade.market.aggregation.core.AggregationBucket;
import com.qyl.v2trade.market.aggregation.core.PeriodCalculator;
import com.qyl.v2trade.market.aggregation.event.AggregatedKLine;
import com.qyl.v2trade.market.aggregation.persistence.AggregatedKLineStorageService;
import com.qyl.v2trade.market.model.NormalizedKline;
import com.qyl.v2trade.market.model.event.KlineEvent;
import com.qyl.v2trade.market.web.query.MarketQueryService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * 批量聚合控制器
 * 
 * <p>提供按时间范围批量聚合K线的功能
 * 
 * <p>功能：
 * <ul>
 *   <li>从QuestDB查询指定时间范围内的1m K线数据</li>
 *   <li>聚合成5m、15m、30m、1h、4h周期</li>
 *   <li>保存聚合结果到QuestDB</li>
 * </ul>
 *
 * @author qyl
 */
@Slf4j
@RestController
@RequestMapping("/api/market/aggregation/batch")
public class BatchAggregationController {
    
    @Autowired
    @Qualifier("questDbMarketQueryService")
    private MarketQueryService marketQueryService;
    
    @Autowired
    private AggregatedKLineStorageService storageService;
    
    /**
     * 批量聚合请求参数
     */
    @Data
    public static class BatchAggregationRequest {
        /**
         * 交易对符号（如：BTC-USDT-SWAP）
         */
        private String symbol;
        
        /**
         * 开始时间戳（毫秒）
         */
        private Long startTime;
        
        /**
         * 结束时间戳（毫秒）
         */
        private Long endTime;
        
        /**
         * 交易所名称（可选，默认：OKX）
         */
        private String exchange = "OKX";
        
        /**
         * 是否保存到数据库（默认：true）
         */
        private Boolean saveToDb = true;
    }
    
    /**
     * 批量聚合响应结果
     */
    @Data
    public static class BatchAggregationResponse {
        /**
         * 查询到的1m K线数量
         */
        private int sourceKlineCount;
        
        /**
         * 各周期聚合结果统计
         */
        private Map<String, PeriodAggregationResult> periodResults = new HashMap<>();
        
        /**
         * 总耗时（毫秒）
         */
        private long durationMs;
    }
    
    /**
     * 周期聚合结果
     */
    @Data
    public static class PeriodAggregationResult {
        /**
         * 周期（如：5m, 15m）
         */
        private String period;
        
        /**
         * 聚合生成的K线数量
         */
        private int aggregatedCount;
        
        /**
         * 成功保存到数据库的数量
         */
        private int savedCount;
        
        /**
         * 跳过数量（已存在）
         */
        private int skippedCount;
    }
    
    /**
     * 批量聚合K线
     * 
     * <p>从QuestDB查询指定时间范围内的1m K线数据，聚合成5m、15m、30m、1h、4h周期
     * 
     * <p>请求示例：
     * <pre>
     * POST /api/market/aggregation/batch/aggregate
     * {
     *   "symbol": "BTC-USDT-SWAP",
     *   "startTime": 1704067200000,
     *   "endTime": 1704153600000,
     *   "exchange": "OKX",
     *   "saveToDb": true
     * }
     * </pre>
     * 
     * @param request 批量聚合请求
     * @return 聚合结果统计
     */
    @PostMapping("/aggregate")
    public Result<BatchAggregationResponse> batchAggregate(@RequestBody BatchAggregationRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 参数校验
            if (request.getSymbol() == null || request.getSymbol().trim().isEmpty()) {
                return Result.error("交易对符号不能为空");
            }
            if (request.getStartTime() == null || request.getEndTime() == null) {
                return Result.error("开始时间和结束时间不能为空");
            }
            if (request.getStartTime() >= request.getEndTime()) {
                return Result.error("开始时间必须小于结束时间");
            }
            
            String symbol = request.getSymbol().trim();
            long fromTimestamp = request.getStartTime();
            long toTimestamp = request.getEndTime();
            String exchange = request.getExchange() != null ? request.getExchange() : "OKX";
            boolean saveToDb = request.getSaveToDb() != null ? request.getSaveToDb() : true;
            
            log.info("开始批量聚合: symbol={}, from={}, to={}, exchange={}, saveToDb={}", 
                    symbol, fromTimestamp, toTimestamp, exchange, saveToDb);
            
            // 1. 从QuestDB查询1m K线数据
            List<NormalizedKline> sourceKlines = marketQueryService.queryKlines(
                    symbol, "1m", fromTimestamp, toTimestamp, null);
            
            if (sourceKlines == null || sourceKlines.isEmpty()) {
                log.warn("未查询到1m K线数据: symbol={}, from={}, to={}", symbol, fromTimestamp, toTimestamp);
                BatchAggregationResponse response = new BatchAggregationResponse();
                response.setSourceKlineCount(0);
                response.setDurationMs(System.currentTimeMillis() - startTime);
                return Result.success("未查询到数据", response);
            }
            
            // 按时间戳排序（确保按时间顺序处理）
            sourceKlines.sort(Comparator.comparing(NormalizedKline::getTimestamp));
            
            log.info("查询到{}根1m K线数据，开始聚合", sourceKlines.size());
            
            // 2. 为每个周期创建聚合Bucket
            Map<SupportedPeriod, Map<String, AggregationBucket>> periodBuckets = new HashMap<>();
            for (SupportedPeriod period : SupportedPeriod.values()) {
                periodBuckets.put(period, new HashMap<>());
            }
            
            // 3. 处理每根1m K线，聚合到各个周期
            int processedCount = 0;
            List<AggregatedKLine> allAggregatedKLines = new ArrayList<>();
            
            for (NormalizedKline kline : sourceKlines) {
                // 转换为KlineEvent
                KlineEvent event = convertToKlineEvent(kline, exchange);
                
                // 为每个周期处理
                for (SupportedPeriod period : SupportedPeriod.values()) {
                    // 计算窗口起始和结束时间
                    long windowStart = PeriodCalculator.calculateWindowStart(kline.getTimestamp(), period);
                    long windowEnd = PeriodCalculator.calculateWindowEnd(windowStart, period);
                    
                    // 生成Bucket Key
                    String bucketKey = generateBucketKey(symbol, period.getPeriod(), windowStart);
                    
                    // 获取或创建Bucket
                    Map<String, AggregationBucket> buckets = periodBuckets.get(period);
                    AggregationBucket bucket = buckets.computeIfAbsent(bucketKey, key -> {
                        return new AggregationBucket(symbol, period.getPeriod(), windowStart, windowEnd);
                    });
                    
                    // 更新Bucket
                    boolean windowComplete = bucket.update(event);
                    
                    // 如果窗口完成，生成聚合结果
                    if (windowComplete) {
                        AggregatedKLine aggregated = bucket.toAggregatedKLine();
                        if (aggregated != null) {
                            allAggregatedKLines.add(aggregated);
                            // 从Map中移除已完成的Bucket
                            buckets.remove(bucketKey);
                        }
                    }
                }
                
                processedCount++;
            }
            
            // 4. 处理剩余的未完成窗口（时间范围结束时的部分窗口）
            for (Map.Entry<SupportedPeriod, Map<String, AggregationBucket>> entry : periodBuckets.entrySet()) {
                SupportedPeriod period = entry.getKey();
                Map<String, AggregationBucket> buckets = entry.getValue();
                
                for (AggregationBucket bucket : buckets.values()) {
                    // 如果Bucket有数据，也生成聚合结果（即使窗口未完全关闭）
                    AggregatedKLine aggregated = bucket.toAggregatedKLine();
                    if (aggregated != null) {
                        allAggregatedKLines.add(aggregated);
                    }
                }
            }
            
            log.info("聚合完成: 处理{}根1m K线，生成{}根聚合K线", 
                    processedCount, allAggregatedKLines.size());
            
            // 5. 按周期分组统计
            Map<String, PeriodAggregationResult> periodResults = new HashMap<>();
            Map<String, List<AggregatedKLine>> groupedByPeriod = new HashMap<>();
            
            for (AggregatedKLine aggregated : allAggregatedKLines) {
                groupedByPeriod.computeIfAbsent(aggregated.period(), k -> new ArrayList<>())
                        .add(aggregated);
            }
            
            // 6. 保存到数据库（如果需要）
            for (Map.Entry<String, List<AggregatedKLine>> entry : groupedByPeriod.entrySet()) {
                String period = entry.getKey();
                List<AggregatedKLine> aggregatedKLines = entry.getValue();
                
                PeriodAggregationResult result = new PeriodAggregationResult();
                result.setPeriod(period);
                result.setAggregatedCount(aggregatedKLines.size());
                
                if (saveToDb) {
                    int savedCount = storageService.batchSave(aggregatedKLines);
                    result.setSavedCount(savedCount);
                    result.setSkippedCount(aggregatedKLines.size() - savedCount);
                    log.info("周期{}: 聚合{}根，保存{}根，跳过{}根", 
                            period, aggregatedKLines.size(), savedCount, result.getSkippedCount());
                } else {
                    result.setSavedCount(0);
                    result.setSkippedCount(0);
                    log.info("周期{}: 聚合{}根（未保存到数据库）", period, aggregatedKLines.size());
                }
                
                periodResults.put(period, result);
            }
            
            // 7. 构建响应
            BatchAggregationResponse response = new BatchAggregationResponse();
            response.setSourceKlineCount(sourceKlines.size());
            response.setPeriodResults(periodResults);
            response.setDurationMs(System.currentTimeMillis() - startTime);
            
            log.info("批量聚合完成: symbol={}, 耗时{}ms, 1m K线{}根, 聚合K线{}根", 
                    symbol, response.getDurationMs(), sourceKlines.size(), allAggregatedKLines.size());
            
            return Result.success("批量聚合完成", response);
            
        } catch (Exception e) {
            log.error("批量聚合失败: symbol={}, startTime={}, endTime={}", 
                    request.getSymbol(), request.getStartTime(), request.getEndTime(), e);
            return Result.error("批量聚合失败: " + e.getMessage());
        }
    }
    
    /**
     * 将NormalizedKline转换为KlineEvent
     */
    private KlineEvent convertToKlineEvent(NormalizedKline kline, String exchange) {
        long openTime = kline.getTimestamp();
        long closeTime = openTime + 60000; // 1分钟K线
        
        return KlineEvent.of(
                kline.getSymbol(),
                exchange,
                openTime,
                closeTime,
                kline.getInterval(),
                BigDecimal.valueOf(kline.getOpen()),
                BigDecimal.valueOf(kline.getHigh()),
                BigDecimal.valueOf(kline.getLow()),
                BigDecimal.valueOf(kline.getClose()),
                BigDecimal.valueOf(kline.getVolume()),
                true, // 历史数据都是已完成的
                System.currentTimeMillis()
        );
    }
    
    /**
     * 生成Bucket Key
     */
    private String generateBucketKey(String symbol, String period, long windowStart) {
        return symbol + "_" + period + "_" + windowStart;
    }
}

