package com.qyl.v2trade.business.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qyl.v2trade.business.system.mapper.ExchangeMarketPairMapper;
import com.qyl.v2trade.business.system.model.entity.ExchangeMarketPair;
import com.qyl.v2trade.business.system.service.ExchangeMarketPairService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 交易所交易规则服务实现类
 */
@Service
public class ExchangeMarketPairServiceImpl extends ServiceImpl<ExchangeMarketPairMapper, ExchangeMarketPair> 
        implements ExchangeMarketPairService {

    private static final Logger logger = LoggerFactory.getLogger(ExchangeMarketPairServiceImpl.class);

    @Override
    public ExchangeMarketPair getByExchangeAndTradingPairId(String exchangeCode, Long tradingPairId) {
        logger.debug("查询交易规则: exchangeCode={}, tradingPairId={}", exchangeCode, tradingPairId);
        
        return getOne(new LambdaQueryWrapper<ExchangeMarketPair>()
                .eq(ExchangeMarketPair::getExchangeCode, exchangeCode)
                .eq(ExchangeMarketPair::getTradingPairId, tradingPairId));
    }

    @Override
    public ExchangeMarketPair getByExchangeAndSymbol(String exchangeCode, String symbolOnExchange) {
        logger.debug("查询交易规则: exchangeCode={}, symbolOnExchange={}", exchangeCode, symbolOnExchange);
        
        return getOne(new LambdaQueryWrapper<ExchangeMarketPair>()
                .eq(ExchangeMarketPair::getExchangeCode, exchangeCode)
                .eq(ExchangeMarketPair::getSymbolOnExchange, symbolOnExchange));
    }

    @Override
    public List<ExchangeMarketPair> listByTradingPairId(Long tradingPairId) {
        logger.debug("查询交易规则列表: tradingPairId={}", tradingPairId);
        
        return list(new LambdaQueryWrapper<ExchangeMarketPair>()
                .eq(ExchangeMarketPair::getTradingPairId, tradingPairId)
                .orderByAsc(ExchangeMarketPair::getExchangeCode));
    }

    @Override
    public List<ExchangeMarketPair> listByExchangeCode(String exchangeCode) {
        logger.debug("查询交易规则列表: exchangeCode={}", exchangeCode);
        
        return list(new LambdaQueryWrapper<ExchangeMarketPair>()
                .eq(ExchangeMarketPair::getExchangeCode, exchangeCode)
                .orderByAsc(ExchangeMarketPair::getSymbolOnExchange));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExchangeMarketPair saveOrUpdateByKey(ExchangeMarketPair exchangeMarketPair) {
        logger.info("保存或更新交易规则: exchangeCode={}, tradingPairId={}", 
                exchangeMarketPair.getExchangeCode(), exchangeMarketPair.getTradingPairId());

        ExchangeMarketPair existing = getByExchangeAndTradingPairId(
                exchangeMarketPair.getExchangeCode(), 
                exchangeMarketPair.getTradingPairId());

        if (existing != null) {
            exchangeMarketPair.setId(existing.getId());
            updateById(exchangeMarketPair);
            logger.info("更新交易规则成功: id={}", existing.getId());
        } else {
            save(exchangeMarketPair);
            logger.info("新增交易规则成功: id={}", exchangeMarketPair.getId());
        }

        return exchangeMarketPair;
    }
}

