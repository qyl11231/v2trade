package com.qyl.v2trade.market.subscription.collector.ingestor;

/**
 * 价格订阅采集接口
 * 负责从交易所WebSocket采集价格数据（ticker频道）
 */
public interface PriceIngestor {

    /**
     * 启动价格订阅采集
     */
    void start();

    /**
     * 停止价格订阅采集
     */
    void stop();

    /**
     * 订阅交易对价格
     * 
     * @param tradingPairId 交易对ID
     * @param symbolOnExchange 交易所交易对标识（如：BTC-USDT-SWAP）
     * @param standardSymbol 标准化交易对符号（如：BTC-USDT）
     */
    void subscribe(Long tradingPairId, String symbolOnExchange, String standardSymbol);

    /**
     * 取消订阅交易对价格
     * 
     * @param tradingPairId 交易对ID
     */
    void unsubscribe(Long tradingPairId);
}

