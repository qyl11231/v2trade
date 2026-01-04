package com.qyl.v2trade.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qyl.v2trade.business.model.entity.ExchangeMarketPair;

import java.util.List;

/**
 * 交易所交易规则服务接口
 */
public interface ExchangeMarketPairService extends IService<ExchangeMarketPair> {

    /**
     * 根据交易所代码和交易对ID查询规则
     * @param exchangeCode 交易所代码
     * @param tradingPairId 交易对ID
     * @return 交易规则，未找到返回null
     */
    ExchangeMarketPair getByExchangeAndTradingPairId(String exchangeCode, Long tradingPairId);

    /**
     * 根据交易所代码和交易所内部标识查询规则
     * @param exchangeCode 交易所代码
     * @param symbolOnExchange 交易所内部标识
     * @return 交易规则
     */
    ExchangeMarketPair getByExchangeAndSymbol(String exchangeCode, String symbolOnExchange);

    /**
     * 根据交易对ID查询所有交易所规则
     * @param tradingPairId 交易对ID
     * @return 交易规则列表
     */
    List<ExchangeMarketPair> listByTradingPairId(Long tradingPairId);

    /**
     * 根据交易所代码查询所有交易规则
     * @param exchangeCode 交易所代码
     * @return 交易规则列表
     */
    List<ExchangeMarketPair> listByExchangeCode(String exchangeCode);

    /**
     * 保存或更新交易规则（根据exchangeCode+tradingPairId唯一键）
     * @param exchangeMarketPair 交易规则
     * @return 保存/更新后的交易规则
     */
    ExchangeMarketPair saveOrUpdateByKey(ExchangeMarketPair exchangeMarketPair);
}

