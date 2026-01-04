package com.qyl.v2trade.market.calibration.service;

import com.qyl.v2trade.market.calibration.service.dto.TradingPairInfo;

/**
 * 交易对信息获取服务接口
 */
public interface TradingPairInfoService {

    /**
     * 获取交易对信息
     * @param tradingPairId 交易对ID
     * @return 交易对信息，未找到返回null
     */
    TradingPairInfo getTradingPairInfo(Long tradingPairId);
}

