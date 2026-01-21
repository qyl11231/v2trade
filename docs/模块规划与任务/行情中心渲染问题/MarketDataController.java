package com.qyl.v2trade.market.web.controller;

import com.qyl.v2trade.common.Result;
import com.qyl.v2trade.common.constants.KlineInterval;
import com.qyl.v2trade.common.util.TimeUtil;
import com.qyl.v2trade.market.model.NormalizedKline;
import com.qyl.v2trade.market.model.dto.KlineQueryRequest;
import com.qyl.v2trade.market.model.dto.KlineResponse;
import com.qyl.v2trade.market.model.dto.TodayStatsResponse;
import com.qyl.v2trade.market.web.query.MarketQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 行情数据控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/market")
public class MarketDataController {

    @Autowired
    private MarketQueryService marketQueryService;

    /**
     * 查询K线数据
     * 
     * GET /api/market/kline?symbol=BTC-USDT&interval=1m&from=1710000000000&to=1710086400000&limit=1000
     */
    @GetMapping("/kline")
    public Result<List<KlineResponse>> queryKlines(@Validated KlineQueryRequest request) {
         log.debug("查询K线: symbol={}, interval={}, from={}, to={}, limit={}",
                 request.getSymbol(), request.getInterval(), 
                 request.getFrom(), request.getTo(), request.getLimit());

        try {
            // 验证周期
            if (!KlineInterval.isValid(request.getInterval())) {
                return Result.error(400, "不支持的K线周期: " + request.getInterval());
            }

            // 验证时间范围
            if (request.getFrom() != null && request.getTo() != null 
                && request.getFrom() > request.getTo()) {
                return Result.error(400, "开始时间不能大于结束时间");
            }

            // 查询K线数据
            // 重构：按照时间管理约定，在 Controller 层将 Long 转换为 Instant（边界转换）
            Instant fromTime = request.getFrom() != null ? TimeUtil.fromEpochMilli(request.getFrom()) : null;
            Instant toTime = request.getTo() != null ? TimeUtil.fromEpochMilli(request.getTo()) : null;
            List<NormalizedKline> klines = marketQueryService.queryKlines(
                request.getSymbol(),
                request.getInterval(),
                fromTime,
                toTime,
                request.getLimit()
            );

            // 转换为响应DTO
            List<KlineResponse> responses = klines.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

            return Result.success(responses);

        } catch (Exception e) {
            log.error("查询K线失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 查询最新一根K线
     * 
     * GET /api/market/kline/latest?symbol=BTC-USDT&interval=1m
     */
    @GetMapping("/kline/latest")
    public Result<KlineResponse> queryLatestKline(
            @RequestParam String symbol,
            @RequestParam String interval) {
        
        log.debug("查询最新K线: symbol={}, interval={}", symbol, interval);

        try {
            // 验证周期
            if (!KlineInterval.isValid(interval)) {
                return Result.error(400, "不支持的K线周期: " + interval);
            }

            NormalizedKline kline = marketQueryService.queryLatestKline(symbol, interval);
            
            if (kline == null) {
                return Result.error(404, "未找到K线数据");
            }

            return Result.success(convertToResponse(kline));

        } catch (Exception e) {
            log.error("查询最新K线失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 查询指定时间点的K线
     * 
     * GET /api/market/kline/timestamp?symbol=BTC-USDT&interval=1m&timestamp=1710000000000
     */
    @GetMapping("/kline/timestamp")
    public Result<KlineResponse> queryKlineByTimestamp(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam Long timestamp) {
        
        log.debug("查询指定时间K线: symbol={}, interval={}, timestamp={}", 
                 symbol, interval, timestamp);

        try {
            // 验证周期
            if (!KlineInterval.isValid(interval)) {
                return Result.error(400, "不支持的K线周期: " + interval);
            }

            // 重构：按照时间管理约定，在 Controller 层将 Long 转换为 Instant（边界转换）
            Instant timestampInstant = TimeUtil.fromEpochMilli(timestamp);
            NormalizedKline kline = marketQueryService.queryKlineByTimestamp(
                symbol, interval, timestampInstant
            );
            
            if (kline == null) {
                return Result.error(404, "未找到K线数据");
            }

            return Result.success(convertToResponse(kline));

        } catch (Exception e) {
            log.error("查询指定时间K线失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 查询今日统计信息
     * 
     * GET /api/market/kline/today-stats?symbol=BTC-USDT
     */
    @GetMapping("/kline/today-stats")
    public Result<TodayStatsResponse> getTodayStats(@RequestParam String symbol) {
        log.debug("查询今日统计: symbol={}", symbol);

        try {
            // 获取今日0点时间（UTC Instant）
            // 重构：按照时间管理约定，使用 Instant 进行计算
            Instant now = Instant.now();
            // 将当前时间对齐到今天开始（UTC时区的0点）
            long nowMillis = TimeUtil.toEpochMilli(now);
            long dayMillis = 24 * 60 * 60 * 1000L;
            long todayStartMillis = (nowMillis / dayMillis) * dayMillis;
            Instant todayStart = TimeUtil.fromEpochMilli(todayStartMillis);
            
            // 查询今日所有1m K线
            // 重构：按照时间管理约定，直接传递 Instant 参数
            List<NormalizedKline> klines = marketQueryService.queryKlines(
                symbol, "1m", todayStart, now, null
            );

            if (klines.isEmpty()) {
                // 如果没有今日数据，返回默认值
                NormalizedKline latest = marketQueryService.queryLatestKline(symbol, "1m");
                if (latest == null) {
                    return Result.error(404, "未找到该交易对的数据");
                }
                
                return Result.success(TodayStatsResponse.builder()
                    .symbol(symbol)
                    .todayHigh(latest.getClose())
                    .todayLow(latest.getClose())
                    .todayChange(0.0)
                    .todayVolume(0.0)
                    .currentPrice(latest.getClose())
                    .build());
            }

            // 计算今日统计
            double todayHigh = klines.stream().mapToDouble(NormalizedKline::getHigh).max().orElse(0.0);
            double todayLow = klines.stream().mapToDouble(NormalizedKline::getLow).min().orElse(0.0);
            double todayVolume = klines.stream().mapToDouble(NormalizedKline::getVolume).sum();
            
            // 计算涨跌幅：今日收盘价 vs 今日开盘价
            double todayOpen = klines.get(0).getOpen();
            double todayClose = klines.get(klines.size() - 1).getClose();
            double todayChange = todayOpen != 0 ? ((todayClose - todayOpen) / todayOpen) * 100 : 0.0;

            // 当前价格使用最新K线的收盘价
            double currentPrice = todayClose;

            TodayStatsResponse stats = TodayStatsResponse.builder()
                .symbol(symbol)
                .todayHigh(todayHigh)
                .todayLow(todayLow)
                .todayChange(todayChange)
                .todayVolume(todayVolume)
                .currentPrice(currentPrice)
                .build();

            return Result.success(stats);

        } catch (Exception e) {
            log.error("查询今日统计失败: symbol={}", symbol, e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 转换为响应DTO
     * 
     * <p>在系统边界（Controller层）进行时区转换：
     * - timestamp: 保留 Instant 类型（UTC），用于内部使用和 JSON 序列化
     * - time: 转换为上海时区字符串，用于前端展示
     */
    private KlineResponse convertToResponse(NormalizedKline kline) {
        // 获取 Instant 类型的时间戳
        Instant timestamp = kline.getTimestampInstant();
        if (timestamp == null && kline.getTimestamp() != null) {
            // 兼容旧代码：如果没有 Instant，从 Long 转换
            timestamp = TimeUtil.fromEpochMilli(kline.getTimestamp());
        }
        
        // 在边界进行时区转换：Instant -> 上海时区字符串
        String timeString = timestamp != null ? TimeUtil.formatAsShanghaiString(timestamp) : null;
        
        return KlineResponse.builder()
                .symbol(kline.getSymbol())
                .interval(kline.getInterval())
                .open(kline.getOpen())
                .high(kline.getHigh())
                .low(kline.getLow())
                .close(kline.getClose())
                .volume(kline.getVolume())
                .timestamp(timestamp)  // Instant 类型（UTC）
                .time(timeString)       // String 类型（上海时区）
                .build();
    }
}

