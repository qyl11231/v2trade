package com.qyl.v2trade.market.subscription.infrastructure.config;

import com.qyl.v2trade.market.subscription.collector.channel.impl.KlineChannel;
import com.qyl.v2trade.market.subscription.collector.channel.impl.PriceChannel;
import com.qyl.v2trade.market.subscription.collector.eventbus.MarketEventBus;
import com.qyl.v2trade.market.subscription.collector.eventbus.impl.SimpleMarketEventBus;
import com.qyl.v2trade.market.subscription.collector.router.ChannelRouter;
import com.qyl.v2trade.market.subscription.collector.websocket.ExchangeWebSocketManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 行情 WebSocket 配置类
 * 
 * <p>负责初始化行情订阅模块的核心组件：
 * <ul>
 *   <li>ExchangeWebSocketManager - WebSocket 连接管理</li>
 *   <li>ChannelRouter - 消息路由</li>
 *   <li>KlineChannel - K 线频道</li>
 *   <li>MarketEventBus - 事件总线</li>
 * </ul>
 *
 * @author qyl
 */
@Slf4j
@Configuration
public class MarketWebSocketConfig {

    /**
     * 创建 MarketEventBus Bean
     */
    @Bean
    public MarketEventBus marketEventBus() {
        log.info("创建 MarketEventBus Bean");
        return new SimpleMarketEventBus();
    }

    /**
     * 创建 ChannelRouter Bean
     */
    @Bean
    public ChannelRouter channelRouter() {
        log.info("创建 ChannelRouter Bean");
        return new ChannelRouter();
    }

    /**
     * 创建 KlineChannel Bean
     */
    @Bean
    public KlineChannel klineChannel(MarketEventBus eventBus) {
        log.info("创建 KlineChannel Bean");
        return new KlineChannel();
    }

    /**
     * 创建 ExchangeWebSocketManager Bean
     * 
     * <p>注册所有 Channel 到 Router
     */
    @Bean
    public ExchangeWebSocketManager exchangeWebSocketManager(
            ChannelRouter channelRouter,
            KlineChannel klineChannel,
            PriceChannel priceChannel) {
        log.info("创建 ExchangeWebSocketManager Bean");

        // 注册 Channel 到 Router
        channelRouter.registerChannel(klineChannel);
        log.info("已注册 Channel: {}", klineChannel.channelType());

        channelRouter.registerChannel(priceChannel);
        log.info("已注册 Channel: {}", priceChannel.channelType());

        return new ExchangeWebSocketManager();
    }
}

