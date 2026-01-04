package com.qyl.v2trade.market.web.controller;

import com.qyl.v2trade.common.Result;
import com.qyl.v2trade.common.constants.KlineInterval;
import com.qyl.v2trade.common.util.UtcTimeConverter;
import com.qyl.v2trade.market.model.NormalizedKline;
import com.qyl.v2trade.market.model.dto.KlineQueryRequest;
import com.qyl.v2trade.market.model.dto.KlineResponse;
import com.qyl.v2trade.market.model.dto.TodayStatsResponse;
import com.qyl.v2trade.market.web.query.MarketQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
            List<NormalizedKline> klines = marketQueryService.queryKlines(
                request.getSymbol(),
                request.getInterval(),
                request.getFrom(),
                request.getTo(),
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

            NormalizedKline kline = marketQueryService.queryKlineByTimestamp(
                symbol, interval, timestamp
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
            // 获取今日0点时间戳（毫秒）
            long now = System.currentTimeMillis();
            long todayStart = (now / (24 * 60 * 60 * 1000L)) * (24 * 60 * 60 * 1000L);
            
            // 查询今日所有1m K线
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
     */
    private KlineResponse convertToResponse(NormalizedKline kline) {
        // 使用 UtcTimeConverter 将UTC时间戳转换为时间字符串（本地时间，默认UTC+8）
        String timeString = UtcTimeConverter.utcTimestampToLocalString(kline.getTimestamp());
        
        return KlineResponse.builder()
                .symbol(kline.getSymbol())
                .interval(kline.getInterval())
                .open(kline.getOpen())
                .high(kline.getHigh())
                .low(kline.getLow())
                .close(kline.getClose())
                .volume(kline.getVolume())
                .timestamp(kline.getTimestamp())
                .timeString(timeString)
                .build();
    }
}

