package com.qyl.v2trade.business.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qyl.v2trade.business.strategy.mapper.StrategySymbolMapper;
import com.qyl.v2trade.business.strategy.model.entity.StrategySymbol;
import com.qyl.v2trade.business.strategy.service.StrategyDefinitionService;
import com.qyl.v2trade.business.strategy.service.StrategySymbolService;
import com.qyl.v2trade.common.constants.EnabledStatus;
import com.qyl.v2trade.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 策略交易对服务实现类
 */
@Service
public class StrategySymbolServiceImpl extends ServiceImpl<StrategySymbolMapper, StrategySymbol> implements StrategySymbolService {

    private static final Logger logger = LoggerFactory.getLogger(StrategySymbolServiceImpl.class);

    @Autowired
    @Lazy
    private StrategyDefinitionService strategyDefinitionService;

    @Override
    public List<StrategySymbol> listByStrategyId(Long strategyId) {
        logger.debug("查询策略交易对列表: strategyId={}", strategyId);
        
        return list(new LambdaQueryWrapper<StrategySymbol>()
                .eq(StrategySymbol::getStrategyId, strategyId)
                .orderByDesc(StrategySymbol::getCreatedAt));
    }

    @Override
    public List<StrategySymbol> listByUserId(Long userId) {
        logger.debug("查询用户策略交易对列表: userId={}", userId);
        
        return list(new LambdaQueryWrapper<StrategySymbol>()
                .eq(StrategySymbol::getUserId, userId)
                .orderByDesc(StrategySymbol::getCreatedAt));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StrategySymbol createStrategySymbol(Long userId, Long strategyId, Long tradingPairId, Integer enabled) {
        logger.info("创建策略交易对: userId={}, strategyId={}, tradingPairId={}", userId, strategyId, tradingPairId);

        // 校验策略是否存在且属于该用户
        try {
            strategyDefinitionService.getStrategyById(strategyId, userId);
        } catch (BusinessException e) {
            throw new BusinessException(404, "策略不存在或无权限");
        }

        // 检查是否已存在相同的策略交易对（strategy_id + trading_pair_id 唯一）
        StrategySymbol existSymbol = getOne(new LambdaQueryWrapper<StrategySymbol>()
                .eq(StrategySymbol::getStrategyId, strategyId)
                .eq(StrategySymbol::getTradingPairId, tradingPairId));
        
        if (existSymbol != null) {
            throw new BusinessException(400, "该交易对已绑定到此策略");
        }

        StrategySymbol symbol = new StrategySymbol();
        symbol.setUserId(userId);
        symbol.setStrategyId(strategyId);
        symbol.setTradingPairId(tradingPairId);
        symbol.setEnabled(enabled != null && enabled.equals(EnabledStatus.ENABLED) ? EnabledStatus.ENABLED : EnabledStatus.DISABLED);

        save(symbol);
        
        logger.info("策略交易对创建成功: id={}", symbol.getId());
        return symbol;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StrategySymbol updateStrategySymbol(Long id, Long userId, Integer enabled) {
        logger.info("更新策略交易对: id={}, userId={}", id, userId);

        StrategySymbol symbol = getById(id);
        if (symbol == null) {
            throw new BusinessException(404, "策略交易对不存在");
        }

        // 权限校验：只能更新自己的策略交易对
        if (!symbol.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该策略交易对");
        }

        if (enabled != null) {
            symbol.setEnabled(enabled.equals(EnabledStatus.ENABLED) ? EnabledStatus.ENABLED : EnabledStatus.DISABLED);
        }

        updateById(symbol);
        
        logger.info("策略交易对更新成功: id={}", id);
        return symbol;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStrategySymbol(Long id, Long userId) {
        logger.info("删除策略交易对: id={}, userId={}", id, userId);

        StrategySymbol symbol = getById(id);
        if (symbol == null) {
            throw new BusinessException(404, "策略交易对不存在");
        }

        // 权限校验：只能删除自己的策略交易对
        if (!symbol.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该策略交易对");
        }

        removeById(id);
        
        logger.info("策略交易对删除成功: id={}", id);
    }
}

