package com.qyl.v2trade.market.calibration.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.qyl.v2trade.client.OkxApiClient;
import com.qyl.v2trade.market.calibration.service.HistoricalKlineFetcher;
import com.qyl.v2trade.market.calibration.util.KlineTimeCalculator;
import com.qyl.v2trade.market.model.NormalizedKline;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 历史K线数据拉取服务实现类
 */
@Slf4j
@Service
public class HistoricalKlineFetcherImpl implements HistoricalKlineFetcher {

    @Autowired
    private OkxApiClient okxApiClient;

    private static final int MAX_BATCH_SIZE = 300; // OKX API每次最多返回300条
    private static final long RATE_LIMIT_DELAY_MS = 200; // 限流延迟100ms

    @Override
    public List<NormalizedKline> fetchHistoricalKlines(String symbolOnExchange, List<Long> timestamps) {
        log.info("开始拉取历史K线数据: symbol={}, 时间点数量={}", symbolOnExchange, timestamps.size());
        if (timestamps == null || timestamps.isEmpty()) {
            return new ArrayList<>();
        }
        Long endTimeStamp = timestamps.get(timestamps.size() - 1)+60000+60000*60*8;
        Long startTimestamp = timestamps.get(0)+60000*60*8;
        // 使用Map存储，时间戳为key保证唯一性，最后转成List
        Map<Long, NormalizedKline> klineMap = new HashMap<>();
        try {
            // 第一个时间戳（最旧的时间戳）
            int batchCount = 0;
            Long queryBefore = startTimestamp-60000;
            long lastTimeStamp = 0;
            while (true) {
                batchCount++;
                // 判断时间戳是否是今天（UTC时间）
                boolean isToday = isTodayInUTC(queryBefore);
                // 计算after参数：before参数加300分钟（after是更旧的数据，所以是before + 300分钟）
                Long queryAfter = queryBefore + (300 * 60 * 1000L); // 300分钟 = 300 * 60 * 1000 毫秒

                // 根据是否是今天选择不同的接口
                List<NormalizedKline> batchKlines;
                if (isToday) {
                    log.info("批次 {} 请求今天数据，使用 /api/v5/market/candles 接口, befroe={},after={} ",
                            batchCount, queryBefore, queryAfter);
                    batchKlines = fetchBatchWithBefore(symbolOnExchange, queryAfter, queryBefore);
                } else {
                    log.info("批次 {} 请求非今天数据，使用 /api/v5/market/history-candles 接口, befroe={},after={}",
                            batchCount, queryBefore, queryAfter);
                    batchKlines = fetchHistoryBatchWithBefore(symbolOnExchange, queryAfter, queryBefore);
                }
                
                if (batchKlines.isEmpty()) {
                    log.info("批次 {} 返回数据为空，停止获取", batchCount);
                    break;
                }
                
                // 记录返回数据的时间范围（使用UTC时区）
                long firstTimestamp = batchKlines.get(0).getTimestamp(); // 最新的（OKX返回是倒序）


                queryBefore = firstTimestamp - 60000;
                // 使用Map存储，时间戳为key保证唯一性
                Map<Long, NormalizedKline> batchKlineMap = new HashMap<>();
                
                for (NormalizedKline kline : batchKlines) {
                    // 对齐到整分钟（去掉秒和毫秒部分）
                    long timestamp = kline.getTimestamp();
                    long myTimeStamp = KlineTimeCalculator.alignToMinuteStart(timestamp);

                    // 只要在开始时间和结束时间范围内就接受
                    if (myTimeStamp >= startTimestamp && myTimeStamp <= endTimeStamp) {
                        // 更新K线的时间戳为对齐后的时间戳
                        kline.setTimestamp(myTimeStamp);
                        kline.setExchangeTimestamp(myTimeStamp);
                        // 使用Map存储，时间戳为key，自动去重
                        batchKlineMap.put(myTimeStamp, kline);
                    }
                }
                
                // 将Map中的K线添加到总Map中（自动去重）
                klineMap.putAll(batchKlineMap);
                
                log.info("批次 {} 处理完成: symbol={}, API返回={}, 范围内有效={}, 累计={}",
                        batchCount, symbolOnExchange, batchKlines.size(), batchKlineMap.size(), klineMap.size());

                if(firstTimestamp == lastTimeStamp){
                    log.info("批次 {} 已达到时间边界，停止获取", batchCount);
                    break;
                }
                // 如果最大值等于或大于当前queryafter，说明没有更新的数据了，停止获取
                if (firstTimestamp >= endTimeStamp) {
                    log.info("批次 {} 已达到时间边界，停止获取", batchCount);
                    break;
                }
                lastTimeStamp = firstTimestamp;
                // 限流：每次请求间隔200ms
                Thread.sleep(RATE_LIMIT_DELAY_MS);
            }

            // 将Map转换为List返回
            List<NormalizedKline> result = new ArrayList<>(klineMap.values());
            log.info("历史K线数据拉取完成: symbol={}, 期望时间点={}, 实际获取={}",
                    symbolOnExchange, timestamps.size(), result.size());
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("拉取历史K线数据被中断: symbol={}", symbolOnExchange, e);
            throw new RuntimeException("拉取历史K线数据被中断", e);
        } catch (Exception e) {
            log.error("拉取历史K线数据失败: symbol={}", symbolOnExchange, e);
            throw new RuntimeException("拉取历史K线数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使用after和before参数拉取K线数据（推荐方法，更可靠）
     * 
     * @param symbolOnExchange 交易所格式symbol
     * @param after 请求此时间戳之后的数据（OKX API参数after，更旧的数据）
     * @param before 请求此时间戳之前的数据（OKX API参数before，更新的数据）
     * @return K线数据列表
     */
    private List<NormalizedKline> fetchBatchWithRange(String symbolOnExchange, Long after, Long before) {
        try {
            // 参数验证
            if (after == null || before == null) {
                log.error("时间参数不能为null: symbol={}, after={}, before={}", symbolOnExchange, after, before);
                return new ArrayList<>();
            }
            
            if (after >= before) {
                log.error("时间范围无效: after({}) >= before({}), symbol={}", after, before, symbolOnExchange);
                return new ArrayList<>();
            }
            
            // 调用OKX API获取K线数据
            // OKX API参数说明：
            // after: 请求此时间戳之后的数据（更旧的历史数据）
            // before: 请求此时间戳之前的数据（更新的数据，接近当前时间）
            // 同时使用after和before可以明确限定时间范围
            // OKX返回的数据是按时间倒序排列的（最新的在前）
            // 将时间戳转换为可读格式（用于日志，使用UTC时区）
            java.time.Instant afterInstant = java.time.Instant.ofEpochMilli(after);
            java.time.Instant beforeInstant = java.time.Instant.ofEpochMilli(before);
            String afterTimeStr = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
                    .format(java.time.ZonedDateTime.ofInstant(afterInstant, java.time.ZoneOffset.UTC));
            String beforeTimeStr = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
                    .format(java.time.ZonedDateTime.ofInstant(beforeInstant, java.time.ZoneOffset.UTC));
            log.info("调用OKX API (after/before方式): symbol={}, after={} ({}), before={} ({}), 时间跨度={}分钟", 
                    symbolOnExchange, after, afterTimeStr, before, beforeTimeStr, (before - after) / 60000);
            JsonNode response = okxApiClient.getKlines(symbolOnExchange, "1m", after, before, MAX_BATCH_SIZE);

            // 解析响应
            if (response == null) {
                log.error("OKX API返回null响应: symbol={}, after={}, before={}", symbolOnExchange, after, before);
                return new ArrayList<>();
            }
            
            // 记录完整响应用于调试
            log.debug("OKX API完整响应: {}", response.toString());
            
            if (!response.has("code")) {
                log.error("OKX API响应缺少code字段: {}", response.toString());
                return new ArrayList<>();
            }
            
            String code = response.get("code").asText();
            if (!"0".equals(code)) {
                String msg = response.has("msg") ? response.get("msg").asText() : "未知错误";
                log.warn("OKX API返回错误: code={}, msg={}, 完整响应={}", code, msg, response.toString());
                return new ArrayList<>();
            }

            JsonNode dataArray = response.get("data");
            if (dataArray == null) {
                log.warn("OKX API返回数据为空: symbol={}, after={}, before={}, 完整响应={}", 
                        symbolOnExchange, after, before, response.toString());
                return new ArrayList<>();
            }
            
            if (!dataArray.isArray()) {
                log.warn("OKX API返回数据格式错误: data字段不是数组, data={}, 完整响应={}", 
                        dataArray.toString(), response.toString());
                return new ArrayList<>();
            }
            
            log.debug("OKX API返回数据数组大小: {}", dataArray.size());

            // 解析K线数据
            // OKX返回格式：["1698768000000","42600.1","42600.1","42600","42600","0.12345","0.12345","1698768059999"]
            // [时间戳(毫秒), 开盘价, 最高价, 最低价, 收盘价, 成交量, 成交额, K线结束时间戳(毫秒)]
            List<NormalizedKline> klines = new ArrayList<>();
            for (JsonNode candle : dataArray) {
                if (candle.isArray() && candle.size() >= 6) {
                    try {
                        long timestamp = Long.parseLong(candle.get(0).asText()); // 时间戳（毫秒）
                        double open = Double.parseDouble(candle.get(1).asText());
                        double high = Double.parseDouble(candle.get(2).asText());
                        double low = Double.parseDouble(candle.get(3).asText());
                        double close = Double.parseDouble(candle.get(4).asText());
                        double volume = Double.parseDouble(candle.get(5).asText());

                        NormalizedKline kline = NormalizedKline.builder()
                                .symbol(symbolOnExchange) // 使用交易所格式的symbol
                                .interval("1m")
                                .open(open)
                                .high(high)
                                .low(low)
                                .close(close)
                                .volume(volume)
                                .timestamp(timestamp)
                                .exchangeTimestamp(timestamp)
                                .build();

                        klines.add(kline);
                    } catch (Exception e) {
                        log.warn("解析K线数据失败: candle={}", candle, e);
                    }
                }
            }

            log.debug("OKX API返回数据: symbol={}, after={}, before={}, 实际返回={}条", 
                    symbolOnExchange, after, before, klines.size());
            
            if (klines.isEmpty()) {
                log.warn("OKX API返回空数据: symbol={}, after={} ({}), before={} ({})，可能是该时间范围内没有数据或数据不存在", 
                        symbolOnExchange, after, afterTimeStr, before, beforeTimeStr);
            } else {
                // 记录返回数据的时间范围
                long firstTimestamp = klines.get(0).getTimestamp();
                long lastTimestamp = klines.get(klines.size() - 1).getTimestamp();
                log.info("OKX API返回数据时间范围: 最新={}, 最旧={}, 共{}条", 
                        firstTimestamp, lastTimestamp, klines.size());
            }
            
            return klines;
        } catch (Exception e) {
            log.error("拉取批次K线数据失败: symbol={}, after={}, before={}", 
                    symbolOnExchange, after, before, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 判断时间戳是否是今天（UTC时间）
     * 
     * @param timestamp 时间戳（毫秒）
     * @return 是否是今天
     */
    private boolean isTodayInUTC(Long timestamp) {
        java.time.Instant timestampInstant = java.time.Instant.ofEpochMilli(timestamp);
        java.time.LocalDate timestampDate = timestampInstant.atZone(java.time.ZoneOffset.UTC).toLocalDate();
        
        java.time.Instant nowInstant = java.time.Instant.now();
        java.time.LocalDate todayDate = nowInstant.atZone(java.time.ZoneOffset.UTC).toLocalDate();
        
        return timestampDate.equals(todayDate);
    }
    
    /**
     * 使用after和before参数拉取历史K线数据（非今天数据，使用history-candles接口）
     * 
     * @param symbolOnExchange 交易所格式symbol
     * @param after 请求此时间戳之后的数据（OKX API参数after，更旧的数据）
     * @param before 请求此时间戳之前的数据（OKX API参数before，更新的数据）
     * @return K线数据列表
     */
    private List<NormalizedKline> fetchHistoryBatchWithBefore(String symbolOnExchange, Long after, Long before) {
        try {
            // 调用OKX历史K线API获取数据
            java.time.Instant afterInstant = java.time.Instant.ofEpochMilli(after);
            java.time.Instant beforeInstant = java.time.Instant.ofEpochMilli(before);
            String afterTimeStr = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
                    .format(java.time.ZonedDateTime.ofInstant(afterInstant, java.time.ZoneOffset.UTC));
            String beforeTimeStr = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
                    .format(java.time.ZonedDateTime.ofInstant(beforeInstant, java.time.ZoneOffset.UTC));
            log.info("调用OKX历史K线API (after/before方式): symbol={}, after={} ({}), before={} ({})", 
                    symbolOnExchange, after, afterTimeStr, before, beforeTimeStr);
            JsonNode response = okxApiClient.getHistoryCandles(symbolOnExchange, "1m", after, before, MAX_BATCH_SIZE);

            // 解析响应
            if (response == null) {
                log.error("OKX历史K线API返回null响应: symbol={}, before={}", symbolOnExchange, before);
                return new ArrayList<>();
            }
            
            log.debug("OKX历史K线API完整响应: {}", response.toString());
            
            if (!response.has("code")) {
                log.error("OKX历史K线API响应缺少code字段: {}", response.toString());
                return new ArrayList<>();
            }
            
            String code = response.get("code").asText();
            if (!"0".equals(code)) {
                String msg = response.has("msg") ? response.get("msg").asText() : "未知错误";
                log.warn("OKX历史K线API返回错误: code={}, msg={}, 完整响应={}", code, msg, response.toString());
                return new ArrayList<>();
            }

            JsonNode dataArray = response.get("data");
            if (dataArray == null) {
                log.warn("OKX历史K线API返回数据为空: symbol={}, before={}, 完整响应={}", 
                        symbolOnExchange, before, response.toString());
                return new ArrayList<>();
            }
            
            if (!dataArray.isArray()) {
                log.warn("OKX历史K线API返回数据格式错误: data字段不是数组, data={}, 完整响应={}", 
                        dataArray.toString(), response.toString());
                return new ArrayList<>();
            }
            
            log.debug("OKX历史K线API返回数据数组大小: {}", dataArray.size());

            // 解析K线数据
            // OKX返回格式：["1698768000000","42600.1","42600.1","42600","42600","0.12345","0.12345","1698768059999"]
            // [时间戳(毫秒), 开盘价, 最高价, 最低价, 收盘价, 成交量, 成交额, K线结束时间戳(毫秒)]
            List<NormalizedKline> klines = new ArrayList<>();
            for (JsonNode candle : dataArray) {
                if (candle.isArray() && candle.size() >= 6) {
                    try {
                        long timestamp = Long.parseLong(candle.get(0).asText()); // 时间戳（毫秒）
                        double open = Double.parseDouble(candle.get(1).asText());
                        double high = Double.parseDouble(candle.get(2).asText());
                        double low = Double.parseDouble(candle.get(3).asText());
                        double close = Double.parseDouble(candle.get(4).asText());
                        double volume = Double.parseDouble(candle.get(5).asText());

                        NormalizedKline kline = NormalizedKline.builder()
                                .symbol(symbolOnExchange)
                                .interval("1m")
                                .open(open)
                                .high(high)
                                .low(low)
                                .close(close)
                                .volume(volume)
                                .timestamp(timestamp)
                                .exchangeTimestamp(timestamp)
                                .build();

                        klines.add(kline);
                    } catch (Exception e) {
                        log.warn("解析历史K线数据失败: candle={}", candle, e);
                    }
                }
            }

            log.debug("OKX历史K线API返回数据: symbol={}, before={}, 实际返回={}条", 
                    symbolOnExchange, before, klines.size());
            
            if (klines.isEmpty()) {
                log.warn("OKX历史K线API返回空数据: symbol={}, before={} ({})，可能是该时间点之前没有数据或数据不存在", 
                        symbolOnExchange, before, beforeTimeStr);
            } else {
                // 记录返回数据的时间范围
                long firstTimestamp = klines.get(0).getTimestamp();
                long lastTimestamp = klines.get(klines.size() - 1).getTimestamp();
                log.info("OKX历史K线API返回数据时间范围: 最新={}, 最旧={}, 共{}条", 
                        firstTimestamp, lastTimestamp, klines.size());
            }
            
            return klines;
        } catch (Exception e) {
            log.error("拉取批次历史K线数据失败: symbol={}, before={}", 
                    symbolOnExchange, before, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 使用after和before参数拉取K线数据（今天数据，使用candles接口）
     * 
     * @param symbolOnExchange 交易所格式symbol
     * @param after 请求此时间戳之后的数据（OKX API参数after，更旧的数据）
     * @param before 请求此时间戳之前的数据（OKX API参数before，更新的数据）
     * @return K线数据列表
     */
    private List<NormalizedKline> fetchBatchWithBefore(String symbolOnExchange, Long after, Long before) {
        try {

            JsonNode response = okxApiClient.getKlines(symbolOnExchange, "1m",before , after, MAX_BATCH_SIZE);
            // 解析响应
            if (response == null || !response.has("code") || !"0".equals(response.get("code").asText())) {
                String msg = response != null && response.has("msg") ? response.get("msg").asText() : "未知错误";
                log.warn("OKX API返回错误: code={}, msg={}", 
                        response != null && response.has("code") ? response.get("code").asText() : "null", msg);
                return new ArrayList<>();
            }

            JsonNode dataArray = response.get("data");
            if (dataArray == null || !dataArray.isArray()) {
                log.warn("OKX API返回数据格式错误: data字段不存在或不是数组");
                return new ArrayList<>();
            }

            // 解析K线数据
            // OKX返回格式：["1698768000000","42600.1","42600.1","42600","42600","0.12345","0.12345","1698768059999"]
            // [时间戳(毫秒), 开盘价, 最高价, 最低价, 收盘价, 成交量, 成交额, K线结束时间戳(毫秒)]
            List<NormalizedKline> klines = new ArrayList<>();
            for (JsonNode candle : dataArray) {
                if (candle.isArray() && candle.size() >= 6) {
                    try {
                        long timestamp = Long.parseLong(candle.get(0).asText()); // 时间戳（毫秒）
                        double open = Double.parseDouble(candle.get(1).asText());
                        double high = Double.parseDouble(candle.get(2).asText());
                        double low = Double.parseDouble(candle.get(3).asText());
                        double close = Double.parseDouble(candle.get(4).asText());
                        double volume = Double.parseDouble(candle.get(5).asText());

                        NormalizedKline kline = NormalizedKline.builder()
                                .symbol(symbolOnExchange) // 使用交易所格式的symbol
                                .interval("1m")
                                .open(open)
                                .high(high)
                                .low(low)
                                .close(close)
                                .volume(volume)
                                .timestamp(timestamp)
                                .exchangeTimestamp(timestamp)
                                .build();

                        klines.add(kline);
                    } catch (Exception e) {
                        log.warn("解析K线数据失败: candle={}", candle, e);
                    }
                }
            }

            log.debug("OKX API返回数据: symbol={}, before={}, 实际返回={}条", 
                    symbolOnExchange, before, klines.size());
            
            if (klines.isEmpty()) {
                log.warn("OKX API返回空数据: symbol={}, before={} ，可能是该时间点之前没有数据或数据不存在",
                        symbolOnExchange, before );
            } else {
                // 记录返回数据的时间范围
                long firstTimestamp = klines.get(0).getTimestamp();
                long lastTimestamp = klines.get(klines.size() - 1).getTimestamp();
                log.info("OKX API返回数据时间范围: 最新={}, 最旧={}, 共{}条", 
                        firstTimestamp, lastTimestamp, klines.size());
            }
            
            return klines;
        } catch (Exception e) {
            log.error("拉取批次K线数据失败: symbol={}, before={}", 
                    symbolOnExchange, before, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 拉取一批K线数据（使用after和before参数，不推荐，仅保留作为备选）
     * 
     * @param symbolOnExchange 交易所格式symbol
     * @param after 请求此时间戳之后的数据（OKX API参数after）
     * @param before 请求此时间戳之前的数据（OKX API参数before）
     * @return K线数据列表
     */
    @Deprecated
    private List<NormalizedKline> fetchBatch(String symbolOnExchange, Long after, Long before) {
        try {
            log.debug("调用OKX API (after/before方式): symbol={}, after={}, before={}", symbolOnExchange, after, before);
            JsonNode response = okxApiClient.getKlines(symbolOnExchange, "1m", after, before, MAX_BATCH_SIZE);

            // 解析响应
            if (response == null || !response.has("code") || !"0".equals(response.get("code").asText())) {
                String msg = response != null && response.has("msg") ? response.get("msg").asText() : "未知错误";
                log.warn("OKX API返回错误: code={}, msg={}", 
                        response != null && response.has("code") ? response.get("code").asText() : "null", msg);
                return new ArrayList<>();
            }

            JsonNode dataArray = response.get("data");
            if (dataArray == null || !dataArray.isArray()) {
                log.warn("OKX API返回数据格式错误: data字段不存在或不是数组");
                return new ArrayList<>();
            }

            // 解析K线数据
            List<NormalizedKline> klines = new ArrayList<>();
            for (JsonNode candle : dataArray) {
                if (candle.isArray() && candle.size() >= 6) {
                    try {
                        long timestamp = Long.parseLong(candle.get(0).asText());
                        double open = Double.parseDouble(candle.get(1).asText());
                        double high = Double.parseDouble(candle.get(2).asText());
                        double low = Double.parseDouble(candle.get(3).asText());
                        double close = Double.parseDouble(candle.get(4).asText());
                        double volume = Double.parseDouble(candle.get(5).asText());

                        NormalizedKline kline = NormalizedKline.builder()
                                .symbol(symbolOnExchange)
                                .interval("1m")
                                .open(open)
                                .high(high)
                                .low(low)
                                .close(close)
                                .volume(volume)
                                .timestamp(timestamp)
                                .exchangeTimestamp(timestamp)
                                .build();

                        klines.add(kline);
                    } catch (Exception e) {
                        log.warn("解析K线数据失败: candle={}", candle, e);
                    }
                }
            }

            log.debug("OKX API返回数据: symbol={}, after={}, before={}, 实际返回={}条", 
                    symbolOnExchange, after, before, klines.size());
            
            return klines;
        } catch (Exception e) {
            log.error("拉取批次K线数据失败: symbol={}, after={}, before={}", 
                    symbolOnExchange, after, before, e);
            return new ArrayList<>();
        }
    }
}

