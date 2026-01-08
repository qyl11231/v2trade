package com.qyl.v2trade.market.subscription.service;

import com.qyl.v2trade.business.system.model.entity.ExchangeMarketPair;
import com.qyl.v2trade.business.system.model.entity.MarketSubscriptionConfig;
import com.qyl.v2trade.business.system.model.entity.TradingPair;
import com.qyl.v2trade.business.system.service.ExchangeMarketPairService;
import com.qyl.v2trade.business.system.service.MarketSubscriptionConfigService;
import com.qyl.v2trade.business.system.service.TradingPairService;
import com.qyl.v2trade.common.constants.ExchangeCode;
import com.qyl.v2trade.market.subscription.collector.channel.impl.PriceChannel;
import com.qyl.v2trade.market.subscription.collector.ingestor.PriceIngestor;
import com.qyl.v2trade.market.subscription.collector.router.ChannelRouter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 价格订阅启动服务
 * 
 * <p>在应用启动时加载价格订阅配置并启动价格订阅。
 * 
 * <p>职责：
 * <ul>
 *   <li>确保PriceChannel已注册到ChannelRouter（通常在配置类中完成）</li>
 *   <li>加载MarketSubscriptionConfig配置</li>
 *   <li>订阅价格频道（ticker）</li>
 * </ul>
 *
 * @author qyl
 */
@Slf4j
@Component
public class PriceSubscriptionService implements ApplicationRunner {

    @Autowired(required = false)
    private PriceIngestor priceIngestor;

    @Autowired(required = false)
    private MarketSubscriptionConfigService subscriptionConfigService;

    @Autowired(required = false)
    private TradingPairService tradingPairService;

    @Autowired(required = false)
    private ExchangeMarketPairService exchangeMarketPairService;

    @Autowired(required = false)
    private PriceChannel priceChannel;

    @Autowired(required = false)
    private ChannelRouter channelRouter;

    @Override
    public void run(ApplicationArguments args) {
        log.info("价格订阅启动服务开始初始化...");

        try {
            // 启动价格订阅服务
            if (priceIngestor != null) {
                priceIngestor.start();
            }

            // 加载并订阅价格
            loadAndSubscribePrice();

            log.info("价格订阅启动服务初始化完成");
        } catch (Exception e) {
            log.error("价格订阅启动服务初始化失败", e);
            // 不抛出异常，允许应用继续启动
        }
    }

    /**
     * 加载并订阅所有启用的交易对价格
     * 
     * <p>参考MarketDataCenter.loadAndSubscribe()方法的实现。
     */
    public void loadAndSubscribePrice() {
        if (subscriptionConfigService == null || tradingPairService == null || 
            exchangeMarketPairService == null || priceIngestor == null) {
            log.warn("价格订阅所需服务未注入，跳过价格订阅初始化");
            return;
        }

        try {
            // 获取所有启用的订阅配置
            List<MarketSubscriptionConfig> configs = subscriptionConfigService.listEnabled();
            log.info("加载价格订阅配置: count={}", configs.size());

            for (MarketSubscriptionConfig config : configs) {
                try {
                    // 获取交易对信息
                    TradingPair tradingPair = tradingPairService.getById(config.getTradingPairId());
                    if (tradingPair == null) {
                        log.warn("交易对不存在: tradingPairId={}", config.getTradingPairId());
                        continue;
                    }

                    // 获取市场类型（SPOT/SWAP/FUTURES）
                    String marketType = tradingPair.getMarketType();
                    if (marketType == null || marketType.trim().isEmpty()) {
                        log.warn("交易对市场类型为空，跳过价格订阅: tradingPairId={}, symbol={}", 
                                config.getTradingPairId(), tradingPair.getSymbol());
                        continue;
                    }

                    // 获取交易所交易规则
                    ExchangeMarketPair exchangePair = exchangeMarketPairService
                            .getByExchangeAndTradingPairId(ExchangeCode.OKX, config.getTradingPairId());
                    if (exchangePair == null) {
                        log.warn("交易所交易规则不存在: tradingPairId={}, marketType={}", 
                                config.getTradingPairId(), marketType);
                        continue;
                    }

                    // 订阅价格（通过PriceIngestor）
                    priceIngestor.subscribe(tradingPair.getId(), exchangePair.getSymbolOnExchange(), tradingPair.getSymbol());
                    
                    log.info("订阅价格成功: tradingPairId={}, symbol={}, marketType={}, symbolOnExchange={}", 
                            tradingPair.getId(), tradingPair.getSymbol(), marketType, exchangePair.getSymbolOnExchange());

                } catch (Exception e) {
                    log.error("订阅价格失败: tradingPairId={}", config.getTradingPairId(), e);
                    // 单个交易对订阅失败不影响其他交易对
                }
            }
        } catch (Exception e) {
            log.error("加载价格订阅配置失败", e);
        }
    }
}

