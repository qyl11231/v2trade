package com.qyl.v2trade.market.subscription.delivery.distributor;

import com.qyl.v2trade.market.model.NormalizedKline;

/**
 * 行情分发服务接口
 * 负责向订阅的客户端推送实时行情数据
 */
public interface MarketDistributor {

    /**
     * 推送K线数据给所有订阅的客户端
     * 
     * @param kline K线数据
     */
    void broadcastKline(NormalizedKline kline);

    /**
     * 推送K线数据给订阅特定交易对的客户端
     * 
     * @param symbol 交易对符号
     * @param kline K线数据
     */
    void broadcastKline(String symbol, NormalizedKline kline);

    /**
     * 获取当前订阅的客户端数量
     * 
     * @param symbol 交易对符号，null表示所有
     * @return 客户端数量
     */
    int getSubscriberCount(String symbol);
}

