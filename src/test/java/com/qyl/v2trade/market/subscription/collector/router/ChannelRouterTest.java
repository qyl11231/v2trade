package com.qyl.v2trade.market.subscription.collector.router;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qyl.v2trade.market.subscription.collector.channel.MarketChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ChannelRouter 测试
 */
class ChannelRouterTest {

    private ChannelRouter channelRouter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        channelRouter = new ChannelRouter();
        try {
            // 使用反射设置objectMapper字段（因为ChannelRouter使用@Autowired）
            java.lang.reflect.Field field = ChannelRouter.class.getDeclaredField("objectMapper");
            field.setAccessible(true);
            field.set(channelRouter, objectMapper);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set objectMapper field", e);
        }
    }

    @Test
    void testTickerChannelRecognition() {
        // 创建测试Channel
        AtomicInteger callCount = new AtomicInteger(0);
        MarketChannel priceChannel = new MarketChannel() {
            @Override
            public String channelType() {
                return MarketChannel.CHANNEL_TYPE_PRICE;
            }

            @Override
            public String buildSubscribeRaw(java.util.Set<String> symbols) {
                return null;
            }

            @Override
            public String buildUnsubscribeRaw(java.util.Set<String> symbols) {
                return null;
            }

            @Override
            public void onMessage(String rawJson) {
                callCount.incrementAndGet();
            }
        };

        channelRouter.registerChannel(priceChannel);

        // 构建ticker消息
        String tickerMessage = "{\"arg\":{\"channel\":\"ticker\",\"instId\":\"BTC-USDT-SWAP\"},\"data\":[{\"instId\":\"BTC-USDT-SWAP\",\"last\":\"42000.5\",\"ts\":\"1710000000000\"}]}";

        // 路由消息
        channelRouter.route(tickerMessage);

        // 验证PriceChannel的onMessage被调用
        assertEquals(1, callCount.get(), "PriceChannel的onMessage应该被调用一次");
    }

    @Test
    void testKlineChannelRecognition() {
        // 创建测试Channel
        AtomicInteger callCount = new AtomicInteger(0);
        MarketChannel klineChannel = new MarketChannel() {
            @Override
            public String channelType() {
                return MarketChannel.CHANNEL_TYPE_KLINE;
            }

            @Override
            public String buildSubscribeRaw(java.util.Set<String> symbols) {
                return null;
            }

            @Override
            public String buildUnsubscribeRaw(java.util.Set<String> symbols) {
                return null;
            }

            @Override
            public void onMessage(String rawJson) {
                callCount.incrementAndGet();
            }
        };

        channelRouter.registerChannel(klineChannel);

        // 构建K线消息
        String klineMessage = "{\"arg\":{\"channel\":\"candle1m\",\"instId\":\"BTC-USDT-SWAP\"},\"data\":[[\"1710000000000\",\"42000.0\",\"42100.0\",\"41950.0\",\"42080.0\",\"123.45\"]]}";

        // 路由消息
        channelRouter.route(klineMessage);

        // 验证KlineChannel的onMessage被调用
        assertEquals(1, callCount.get(), "KlineChannel的onMessage应该被调用一次");
    }

    @Test
    void testSystemMessageFiltering() {
        // 创建测试Channel
        AtomicInteger callCount = new AtomicInteger(0);
        MarketChannel priceChannel = new MarketChannel() {
            @Override
            public String channelType() {
                return MarketChannel.CHANNEL_TYPE_PRICE;
            }

            @Override
            public String buildSubscribeRaw(java.util.Set<String> symbols) {
                return null;
            }

            @Override
            public String buildUnsubscribeRaw(java.util.Set<String> symbols) {
                return null;
            }

            @Override
            public void onMessage(String rawJson) {
                callCount.incrementAndGet();
            }
        };

        channelRouter.registerChannel(priceChannel);

        // 构建系统消息（订阅确认）
        String systemMessage = "{\"event\":\"subscribe\",\"arg\":{\"channel\":\"ticker\",\"instId\":\"BTC-USDT-SWAP\"},\"code\":\"0\",\"msg\":\"\"}";

        // 路由消息
        channelRouter.route(systemMessage);

        // 验证PriceChannel的onMessage不应该被调用（系统消息被过滤）
        assertEquals(0, callCount.get(), "系统消息应该被过滤，PriceChannel的onMessage不应该被调用");
    }

    @Test
    void testUnknownChannel() {
        // 构建未知频道消息
        String unknownMessage = "{\"arg\":{\"channel\":\"unknown\",\"instId\":\"BTC-USDT-SWAP\"},\"data\":[]}";

        // 路由消息（不应该抛出异常）
        assertDoesNotThrow(() -> channelRouter.route(unknownMessage));
    }

    @Test
    void testRegisterChannel() {
        MarketChannel channel = new MarketChannel() {
            @Override
            public String channelType() {
                return MarketChannel.CHANNEL_TYPE_PRICE;
            }

            @Override
            public String buildSubscribeRaw(java.util.Set<String> symbols) {
                return null;
            }

            @Override
            public String buildUnsubscribeRaw(java.util.Set<String> symbols) {
                return null;
            }

            @Override
            public void onMessage(String rawJson) {
            }
        };

        channelRouter.registerChannel(channel);

        assertEquals(1, channelRouter.getChannelCount());
    }

    @Test
    void testRegisterNullChannel() {
        // 注册null不应该抛出异常
        assertDoesNotThrow(() -> channelRouter.registerChannel(null));
        assertEquals(0, channelRouter.getChannelCount());
    }
}

