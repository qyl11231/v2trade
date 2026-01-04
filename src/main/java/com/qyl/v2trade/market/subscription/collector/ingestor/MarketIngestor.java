package com.qyl.v2trade.market.subscription.collector.ingestor;

import com.qyl.v2trade.market.model.NormalizedKline;

/**
 * 行情采集接口
 * 负责从交易所WebSocket采集行情数据
 */
public interface MarketIngestor {

    /**
     * 启动行情采集
     */
    void start();

    /**
     * 停止行情采集
     */
    void stop();

    /**
     * 订阅交易对行情
     * 
     * @param tradingPairId 交易对ID
     * @param symbolOnExchange 交易所交易对标识（如：BTC-USDT-SWAP）
     * @param standardSymbol 标准化交易对符号（如：BTC-USDT）
     */
    void subscribe(Long tradingPairId, String symbolOnExchange, String standardSymbol);

    /**
     * 取消订阅交易对行情
     * 
     * @param tradingPairId 交易对ID
     */
    void unsubscribe(Long tradingPairId);

    /**
     * 处理K线数据回调
     */
    interface KlineHandler {
        void onKline(NormalizedKline kline);
    }
}

