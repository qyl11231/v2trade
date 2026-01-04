package com.qyl.v2trade.market.subscription.collector.channel.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qyl.v2trade.market.subscription.collector.channel.MarketChannel;
import com.qyl.v2trade.market.subscription.collector.eventbus.MarketEventBus;
import com.qyl.v2trade.market.model.event.KlineEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * K 线频道实现
 * 
 * <p>负责解析 OKX K 线消息并转换为标准化的 KlineEvent。
 * 
 * <p>OKX K 线消息格式：
 * <pre>
 * {
 *   "arg": {
 *     "channel": "candle1m",
 *     "instId": "BTC-USDT-SWAP"
 *   },
 *   "data": [
 *     [
 *       "1710000000000",  // 时间戳（毫秒）
 *       "42000.0",        // 开盘价
 *       "42100.0",        // 最高价
 *       "41950.0",        // 最低价
 *       "42080.0",        // 收盘价
 *       "123.45",         // 成交量
 *       "5200000.0",      // 成交额（可选）
 *       "1710000059999"   // K线结束时间戳（可选）
 *     ]
 *   ]
 * }
 * </pre>
 *
 * @author qyl
 */
@Slf4j
public class KlineChannel implements MarketChannel {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MarketEventBus eventBus;

    @Override
    public String channelType() {
        return CHANNEL_TYPE_KLINE;
    }

    @Override
    public String buildSubscribeRaw(Set<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            log.warn("订阅符号集合为空");
            return null;
        }

        // OKX 订阅消息格式：{"op":"subscribe","args":[{"channel":"candle1m","instId":"BTC-USDT-SWAP"}]}
        String args = symbols.stream()
                .map(symbol -> String.format("{\"channel\":\"candle1m\",\"instId\":\"%s\"}", symbol))
                .collect(Collectors.joining(","));

        return String.format("{\"op\":\"subscribe\",\"args\":[%s]}", args);
    }

    @Override
    public String buildUnsubscribeRaw(Set<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            log.warn("取消订阅符号集合为空");
            return null;
        }

        // OKX 取消订阅消息格式：{"op":"unsubscribe","args":[{"channel":"candle1m","instId":"BTC-USDT-SWAP"}]}
        String args = symbols.stream()
                .map(symbol -> String.format("{\"channel\":\"candle1m\",\"instId\":\"%s\"}", symbol))
                .collect(Collectors.joining(","));

        return String.format("{\"op\":\"unsubscribe\",\"args\":[%s]}", args);
    }

    @Override
    public void onMessage(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);

            // 检查是否有 data 字段
            JsonNode dataNode = root.path("data");
            if (!dataNode.isArray() || dataNode.size() == 0) {
                log.debug("OKX K线消息中没有数据: {}", rawJson);
                return;
            }

            // 获取 arg 信息
            JsonNode argNode = root.path("arg");
            if (!argNode.has("instId")) {
                log.warn("OKX K线消息缺少 instId: {}", rawJson);
                return;
            }

            String instId = argNode.get("instId").asText();
            String channel = argNode.path("channel").asText();

            // 提取周期（如：candle1m -> 1m）
            String interval = "1m";
            if (channel.startsWith("candle")) {
                interval = channel.substring(6);
            }

            // 处理每根 K 线数据
            for (JsonNode klineArray : dataNode) {
                if (!klineArray.isArray() || klineArray.size() < 6) {
                    log.warn("OKX K线数据格式异常: {}", klineArray);
                    continue;
                }

                // 解析 K 线数据
                // OKX WebSocket 格式：[时间戳(0), 开盘价(1), 最高价(2), 最低价(3), 收盘价(4), 成交量(5), 成交额(6), 其他(7), confirm(8)]
                // 索引8：K线状态，0=未完结，1=已完结
                // 时间戳语义：OKX返回的是UTC epoch millis，需要对齐到分钟起始点
                long rawTimestamp = Long.parseLong(klineArray.get(0).asText());
                // 对齐到分钟起始点（UTC）
                long timestamp = (rawTimestamp / 60000) * 60000;
                BigDecimal open = new BigDecimal(klineArray.get(1).asText());
                BigDecimal high = new BigDecimal(klineArray.get(2).asText());
                BigDecimal low = new BigDecimal(klineArray.get(3).asText());
                BigDecimal close = new BigDecimal(klineArray.get(4).asText());
                BigDecimal volume = new BigDecimal(klineArray.get(5).asText());
                
                // 检查K线是否已完结（confirm字段，索引8）
                boolean isConfirmed = false;
                if (klineArray.size() > 8) {
                    String confirmStr = klineArray.get(8).asText();
                    isConfirmed = "1".equals(confirmStr);
                }
                
                // 如果K线未完结，检查时间戳是否接近分钟末尾（第59秒）
                // 只有在K线已确认，或者在1分钟的第59秒时，才处理数据
                if (!isConfirmed) {
                    long currentTime = System.currentTimeMillis();
                    long klineStartTime = timestamp;
                    long elapsedSeconds = (currentTime - klineStartTime) / 1000;
                    
                    // 如果距离K线开始时间不足59秒，跳过（K线还在更新中）
                    if (elapsedSeconds < 59) {
                        log.debug("跳过未完结K线（距离开始不足59秒）: symbol={}, timestamp={}, elapsedSeconds={}", 
                                instId, timestamp, elapsedSeconds);
                        continue;
                    }
                    
                    // 如果已经超过60秒，说明是下一根K线，也跳过（应该已经处理过了）
                    if (elapsedSeconds >= 60) {
                        log.debug("跳过过期K线（已超过60秒）: symbol={}, timestamp={}, elapsedSeconds={}", 
                                instId, timestamp, elapsedSeconds);
                        continue;
                    }

                    // 第59秒，处理数据
                    log.debug("处理未完结K线（第59秒）: symbol={}, timestamp={}, elapsedSeconds={}",
                            instId, timestamp, elapsedSeconds);
                } else {
                    log.debug("处理已完结K线: symbol={}, timestamp={}", instId, timestamp);
                }
                
                // 记录解析后的数据（用于调试）
                log.debug("OKX K线解析结果: symbol={}, timestamp={}, isConfirmed={}, open={}, high={}, low={}, close={}, volume={}",
                        instId, timestamp, isConfirmed, open, high, low, close, volume);

                // 数据验证：检查开高低收价格是否合理
                // 正常情况下：high >= open, high >= close, low <= open, low <= close
                if (high.compareTo(open) < 0 || high.compareTo(close) < 0 || 
                    low.compareTo(open) > 0 || low.compareTo(close) > 0) {
                    log.warn("OKX K线数据异常（价格关系不合理）: symbol={}, timestamp={}, open={}, high={}, low={}, close={}, raw={}", 
                            instId, timestamp, open, high, low, close, klineArray);
                    // 继续处理，但记录警告
                }
                
                // 如果开高低收都相同，记录警告（可能是未完成的K线或数据异常）
                if (open.equals(high) && high.equals(low) && low.equals(close)) {
                    log.warn("OKX K线数据异常（开高低收价格相同）: symbol={}, timestamp={}, price={}, raw={}", 
                            instId, timestamp, open, klineArray);
                }

                // 计算收盘时间（根据周期计算）
                // 根据周期计算收盘时间
                long closeTime = calculateCloseTime(timestamp, interval);

                // 创建 KlineEvent
                // v1.0 阶段：isFinal 设为 false，由下游 MarketDataCenter 负责判断
                KlineEvent event = KlineEvent.of(
                        instId,                    // symbol（使用交易所原始格式）
                        "OKX",                     // exchange
                        timestamp,                 // openTime
                        closeTime,                 // closeTime
                        interval,                  // interval
                        open,                     // open
                        high,                      // high
                        low,                       // low
                        close,                     // close
                        volume,                    // volume
                        false,                     // isFinal（v1.0 默认 false）
                        System.currentTimeMillis()  // eventTime（本地时间戳）
                );

                // 发布到 EventBus
                eventBus.publish(event);
                log.debug("K线事件已发布: symbol={}, timestamp={}, open={}, high={}, low={}, close={}, volume={}", 
                        instId, timestamp, open, high, low, close, volume);
            }

        } catch (Exception e) {
            log.error("处理 OKX K线消息失败: {}", rawJson, e);
        }
    }

    /**
     * 根据周期计算收盘时间
     * 
     * @param openTime 开盘时间（毫秒）
     * @param interval 周期（如：1m, 5m, 1h）
     * @return 收盘时间（毫秒）
     */
    private long calculateCloseTime(long openTime, String interval) {
        if (interval == null || interval.isEmpty()) {
            return openTime + 60000; // 默认 1 分钟
        }

        try {
            // 提取数字和单位
            String numberStr = interval.replaceAll("[^0-9]", "");
            String unit = interval.replaceAll("[0-9]", "").toLowerCase();

            if (numberStr.isEmpty()) {
                return openTime + 60000; // 默认 1 分钟
            }

            long number = Long.parseLong(numberStr);
            long milliseconds;

            switch (unit) {
                case "m":
                    milliseconds = number * 60 * 1000; // 分钟转毫秒
                    break;
                case "h":
                    milliseconds = number * 60 * 60 * 1000; // 小时转毫秒
                    break;
                case "d":
                    milliseconds = number * 24 * 60 * 60 * 1000; // 天转毫秒
                    break;
                case "w":
                    milliseconds = number * 7 * 24 * 60 * 60 * 1000; // 周转毫秒
                    break;
                case "M":
                    milliseconds = number * 30L * 24 * 60 * 60 * 1000; // 月转毫秒（简化处理，按30天）
                    break;
                default:
                    milliseconds = 60000; // 默认 1 分钟
                    log.warn("未知的周期单位: {}, 使用默认值 1 分钟", unit);
            }

            return openTime + milliseconds;
        } catch (Exception e) {
            log.warn("计算收盘时间失败: interval={}, 使用默认值 1 分钟", interval, e);
            return openTime + 60000; // 默认 1 分钟
        }
    }
}

