package com.qyl.v2trade.market.subscription.delivery.distributor.impl;

import com.qyl.v2trade.common.util.TimeUtil;
import com.qyl.v2trade.market.subscription.delivery.distributor.MarketDistributor;
import com.qyl.v2trade.market.model.NormalizedKline;
import com.qyl.v2trade.market.model.dto.KlineResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * WebSocket行情分发服务实现
 * 使用Spring WebSocket的SimpMessagingTemplate进行消息推送
 */
@Slf4j
@Service
public class WebSocketMarketDistributor implements MarketDistributor {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // 推送路径前缀
    private static final String TOPIC_PREFIX = "/topic/kline";

    @Override
    public void broadcastKline(NormalizedKline kline) {
        try {
            // 转换为响应DTO
            KlineResponse response = convertToResponse(kline);
            
            // 推送给所有订阅的客户端
            // 路径：/topic/kline/{symbol}
            String destination = TOPIC_PREFIX + "/" + kline.getSymbol();
            messagingTemplate.convertAndSend(destination, response);
            
            log.debug("推送K线: symbol={}, timestamp={}", kline.getSymbol(), kline.getTimestamp());
        } catch (Exception e) {
            log.error("推送K线失败: symbol={}", kline.getSymbol(), e);
        }
    }

    @Override
    public void broadcastKline(String symbol, NormalizedKline kline) {
        try {
            KlineResponse response = convertToResponse(kline);
            
            // 推送给订阅特定交易对的客户端
            String destination = TOPIC_PREFIX + "/" + symbol;
            messagingTemplate.convertAndSend(destination, response);
            
            log.debug("推送K线: symbol={}, timestamp={}", symbol, kline.getTimestamp());
        } catch (Exception e) {
            log.error("推送K线失败: symbol={}", symbol, e);
        }
    }

    @Override
    public int getSubscriberCount(String symbol) {
        // Spring WebSocket的SimpMessagingTemplate不直接提供订阅者数量查询
        // 如果需要，可以通过自定义的SessionRegistry来实现
        // 这里先返回-1表示不支持
        return -1;
    }

    /**
     * 转换为响应DTO
     */
    private KlineResponse convertToResponse(NormalizedKline kline) {
        // 获取时间戳（Instant 类型）
        Instant timestamp = kline.getTimestampInstant();
        if (timestamp == null && kline.getTimestamp() != null) {
            timestamp = TimeUtil.fromEpochMilli(kline.getTimestamp());
        }
        
        // 转换为上海时区字符串
        String timeString = timestamp != null ? TimeUtil.formatAsShanghaiString(timestamp) : null;
        
        KlineResponse response = KlineResponse.builder()
                .symbol(kline.getSymbol())
                .interval(kline.getInterval())
                .open(kline.getOpen())
                .high(kline.getHigh())
                .low(kline.getLow())
                .close(kline.getClose())
                .volume(kline.getVolume())
                .timestamp(timestamp)
                .time(timeString)
                .build();
        
        return response;
    }
}

