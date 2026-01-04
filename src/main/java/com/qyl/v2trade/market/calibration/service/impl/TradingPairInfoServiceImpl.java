package com.qyl.v2trade.market.calibration.service.impl;

import com.qyl.v2trade.business.model.entity.ExchangeMarketPair;
import com.qyl.v2trade.business.model.entity.TradingPair;
import com.qyl.v2trade.business.service.ExchangeMarketPairService;
import com.qyl.v2trade.business.service.TradingPairService;
import com.qyl.v2trade.common.constants.ExchangeCode;
import com.qyl.v2trade.exception.BusinessException;
import com.qyl.v2trade.market.calibration.service.TradingPairInfoService;
import com.qyl.v2trade.market.calibration.service.dto.TradingPairInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 交易对信息获取服务实现类
 */
@Service
public class TradingPairInfoServiceImpl implements TradingPairInfoService {

    private static final Logger logger = LoggerFactory.getLogger(TradingPairInfoServiceImpl.class);

    @Autowired
    private TradingPairService tradingPairService;

    @Autowired
    private ExchangeMarketPairService exchangeMarketPairService;

    @Override
    public TradingPairInfo getTradingPairInfo(Long tradingPairId) {
        logger.debug("获取交易对信息: tradingPairId={}", tradingPairId);

        if (tradingPairId == null) {
            throw new BusinessException("交易对ID不能为空");
        }

        // 从 trading_pair 表查询标准化symbol
        TradingPair tradingPair = tradingPairService.getById(tradingPairId);
        if (tradingPair == null) {
            throw new BusinessException("交易对不存在: tradingPairId=" + tradingPairId);
        }

        // 从 exchange_market_pair 表查询交易所格式symbol（默认使用OKX）
        ExchangeMarketPair exchangeMarketPair = exchangeMarketPairService.getByExchangeAndTradingPairId(
                ExchangeCode.OKX, tradingPairId);
        if (exchangeMarketPair == null) {
            throw new BusinessException("交易对在OKX交易所的配置不存在: tradingPairId=" + tradingPairId);
        }

        // 构建返回信息
        TradingPairInfo info = new TradingPairInfo();
        info.setTradingPairId(tradingPairId);
        info.setSymbol(tradingPair.getSymbol());
        info.setSymbolOnExchange(exchangeMarketPair.getSymbolOnExchange());
        info.setExchangeCode(ExchangeCode.OKX);

        logger.debug("获取交易对信息成功: tradingPairId={}, symbol={}, symbolOnExchange={}",
                tradingPairId, info.getSymbol(), info.getSymbolOnExchange());

        return info;
    }
}

