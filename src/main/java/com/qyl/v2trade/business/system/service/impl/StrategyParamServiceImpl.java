package com.qyl.v2trade.business.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qyl.v2trade.business.strategy.mapper.StrategyParamMapper;
import com.qyl.v2trade.business.strategy.model.entity.StrategyParam;
import com.qyl.v2trade.business.strategy.service.StrategyDefinitionService;
import com.qyl.v2trade.business.strategy.service.StrategyParamService;
import com.qyl.v2trade.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 策略参数服务实现类
 */
@Service
public class StrategyParamServiceImpl extends ServiceImpl<StrategyParamMapper, StrategyParam> implements StrategyParamService {

    private static final Logger logger = LoggerFactory.getLogger(StrategyParamServiceImpl.class);

    @Autowired
    @Lazy
    private StrategyDefinitionService strategyDefinitionService;

    @Override
    public StrategyParam getByStrategyId(Long strategyId) {
        logger.debug("查询策略参数: strategyId={}", strategyId);
        
        return getOne(new LambdaQueryWrapper<StrategyParam>()
                .eq(StrategyParam::getStrategyId, strategyId));
    }

    @Override
    public List<StrategyParam> listByUserId(Long userId) {
        logger.debug("查询用户策略参数列表: userId={}", userId);
        
        return list(new LambdaQueryWrapper<StrategyParam>()
                .eq(StrategyParam::getUserId, userId)
                .orderByDesc(StrategyParam::getCreatedAt));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StrategyParam saveOrUpdateParam(Long userId, Long strategyId, BigDecimal initialCapital, 
                                          BigDecimal baseOrderRatio, BigDecimal takeProfitRatio, 
                                          BigDecimal stopLossRatio, String entryCondition, String exitCondition) {
        logger.info("保存或更新策略参数: userId={}, strategyId={}", userId, strategyId);

        // 校验策略是否存在且属于该用户
        try {
            strategyDefinitionService.getStrategyById(strategyId, userId);
        } catch (BusinessException e) {
            throw new BusinessException(404, "策略不存在或无权限");
        }

        // 参数校验
        if (initialCapital == null || initialCapital.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "初始资金必须大于0");
        }
        if (baseOrderRatio == null || baseOrderRatio.compareTo(BigDecimal.ZERO) <= 0 
                || baseOrderRatio.compareTo(BigDecimal.ONE) > 0) {
            throw new BusinessException(400, "单次下单比例必须在0-1之间");
        }

        // 查找是否已存在参数
        StrategyParam param = getByStrategyId(strategyId);
        
        if (param == null) {
            // 创建新参数
            param = new StrategyParam();
            param.setUserId(userId);
            param.setStrategyId(strategyId);
            param.setInitialCapital(initialCapital);
            param.setBaseOrderRatio(baseOrderRatio);
            param.setTakeProfitRatio(takeProfitRatio);
            param.setStopLossRatio(stopLossRatio);
            param.setEntryCondition(entryCondition);
            param.setExitCondition(exitCondition);
            save(param);
            logger.info("策略参数创建成功: paramId={}", param.getId());
        } else {
            // 更新现有参数
            // 权限校验
            if (!param.getUserId().equals(userId)) {
                throw new BusinessException(403, "无权操作该参数");
            }
            param.setInitialCapital(initialCapital);
            param.setBaseOrderRatio(baseOrderRatio);
            param.setTakeProfitRatio(takeProfitRatio);
            param.setStopLossRatio(stopLossRatio);
            param.setEntryCondition(entryCondition);
            param.setExitCondition(exitCondition);
            updateById(param);
            logger.info("策略参数更新成功: paramId={}", param.getId());
        }

        return param;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteParam(Long strategyId, Long userId) {
        logger.info("删除策略参数: strategyId={}, userId={}", strategyId, userId);

        StrategyParam param = getByStrategyId(strategyId);
        if (param == null) {
            throw new BusinessException(404, "策略参数不存在");
        }

        // 权限校验
        if (!param.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该参数");
        }

        removeById(param.getId());
        
        logger.info("策略参数删除成功: strategyId={}", strategyId);
    }
}

