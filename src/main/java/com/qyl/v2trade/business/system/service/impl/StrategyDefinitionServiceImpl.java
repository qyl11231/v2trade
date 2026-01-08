package com.qyl.v2trade.business.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qyl.v2trade.business.strategy.mapper.StrategyDefinitionMapper;
import com.qyl.v2trade.business.system.model.dto.StrategyCreateRequest;
import com.qyl.v2trade.business.system.model.dto.*;
import com.qyl.v2trade.business.strategy.model.entity.StrategyDefinition;
import com.qyl.v2trade.business.strategy.model.entity.StrategyParam;
import com.qyl.v2trade.business.strategy.model.entity.StrategySymbol;
import com.qyl.v2trade.business.strategy.model.entity.StrategySignalSubscription;
import com.qyl.v2trade.business.system.model.entity.TradingPair;
import com.qyl.v2trade.business.strategy.service.StrategyDefinitionService;
import com.qyl.v2trade.business.strategy.service.StrategyParamService;
import com.qyl.v2trade.business.strategy.service.StrategySignalSubscriptionService;
import com.qyl.v2trade.business.strategy.service.StrategySymbolService;
import com.qyl.v2trade.business.signal.service.SignalConfigService;
import com.qyl.v2trade.business.system.service.TradingPairService;
import com.qyl.v2trade.common.constants.ConsumeMode;
import com.qyl.v2trade.common.constants.DecisionMode;
import com.qyl.v2trade.common.constants.EnabledStatus;
import com.qyl.v2trade.common.constants.StrategyType;
import com.qyl.v2trade.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 策略定义服务实现类
 */
@Service
public class StrategyDefinitionServiceImpl extends ServiceImpl<StrategyDefinitionMapper, StrategyDefinition> implements StrategyDefinitionService {

    private static final Logger logger = LoggerFactory.getLogger(StrategyDefinitionServiceImpl.class);

    @Override
    public List<StrategyDefinition> listByUserId(Long userId) {
        logger.debug("查询用户策略定义列表: userId={}", userId);
        
        return list(new LambdaQueryWrapper<StrategyDefinition>()
                .eq(StrategyDefinition::getUserId, userId)
                .orderByDesc(StrategyDefinition::getCreatedAt));
    }

    @Override
    public StrategyDefinition getByUserIdAndName(Long userId, String strategyName) {
        logger.debug("查询策略定义: userId={}, strategyName={}", userId, strategyName);
        
        return getOne(new LambdaQueryWrapper<StrategyDefinition>()
                .eq(StrategyDefinition::getUserId, userId)
                .eq(StrategyDefinition::getStrategyName, strategyName));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StrategyDefinition createStrategy(Long userId, String strategyName, String strategyType, String decisionMode, Integer enabled) {
        logger.info("创建策略定义: userId={}, strategyName={}, strategyType={}", userId, strategyName, strategyType);

        // 校验策略类型
        if (!StrategyType.SIGNAL_DRIVEN.equals(strategyType) 
                && !StrategyType.INDICATOR_DRIVEN.equals(strategyType)
                && !StrategyType.HYBRID.equals(strategyType)) {
            throw new BusinessException(400, "策略类型不合法");
        }

        // 校验决策模式
        if (decisionMode != null && !decisionMode.isEmpty()) {
            if (!DecisionMode.FOLLOW_SIGNAL.equals(decisionMode) 
                    && !DecisionMode.INTENT_DRIVEN.equals(decisionMode)) {
                throw new BusinessException(400, "决策模式不合法");
            }
        }

        // 检查策略名称是否已存在（用户维度唯一）
        StrategyDefinition existStrategy = getByUserIdAndName(userId, strategyName.trim());
        if (existStrategy != null) {
            throw new BusinessException(400, "策略名称已存在");
        }

        StrategyDefinition strategy = new StrategyDefinition();
        strategy.setUserId(userId);
        strategy.setStrategyName(strategyName.trim());
        strategy.setStrategyType(strategyType);
        strategy.setDecisionMode(decisionMode);
        strategy.setEnabled(enabled != null && enabled.equals(EnabledStatus.ENABLED) ? EnabledStatus.ENABLED : EnabledStatus.DISABLED);

        save(strategy);
        
        logger.info("策略定义创建成功: strategyId={}", strategy.getId());
        return strategy;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StrategyDefinition updateStrategy(Long strategyId, Long userId, String strategyName, String strategyType, String decisionMode, Integer enabled) {
        logger.info("更新策略定义: strategyId={}, userId={}", strategyId, userId);

        StrategyDefinition strategy = getById(strategyId);
        if (strategy == null) {
            throw new BusinessException(404, "策略定义不存在");
        }

        // 权限校验：只能更新自己的策略
        if (!strategy.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该策略");
        }

        // 如果更新策略名称，检查是否与其他策略重名
        if (strategyName != null && !strategyName.trim().isEmpty() && !strategyName.equals(strategy.getStrategyName())) {
            StrategyDefinition existStrategy = getByUserIdAndName(userId, strategyName.trim());
            if (existStrategy != null && !existStrategy.getId().equals(strategyId)) {
                throw new BusinessException(400, "策略名称已存在");
            }
            strategy.setStrategyName(strategyName.trim());
        }

        if (strategyType != null && !strategyType.isEmpty()) {
            // 校验策略类型
            if (!StrategyType.SIGNAL_DRIVEN.equals(strategyType) 
                    && !StrategyType.INDICATOR_DRIVEN.equals(strategyType)
                    && !StrategyType.HYBRID.equals(strategyType)) {
                throw new BusinessException(400, "策略类型不合法");
            }
            strategy.setStrategyType(strategyType);
        }

        if (decisionMode != null) {
            // 校验决策模式
            if (!decisionMode.isEmpty()) {
                if (!DecisionMode.FOLLOW_SIGNAL.equals(decisionMode) 
                        && !DecisionMode.INTENT_DRIVEN.equals(decisionMode)) {
                    throw new BusinessException(400, "决策模式不合法");
                }
            }
            strategy.setDecisionMode(decisionMode);
        }

        if (enabled != null) {
            strategy.setEnabled(enabled.equals(EnabledStatus.ENABLED) ? EnabledStatus.ENABLED : EnabledStatus.DISABLED);
        }

        updateById(strategy);
        
        logger.info("策略定义更新成功: strategyId={}", strategyId);
        return strategy;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStrategy(Long strategyId, Long userId) {
        logger.info("删除策略定义: strategyId={}, userId={}", strategyId, userId);

        StrategyDefinition strategy = getById(strategyId);
        if (strategy == null) {
            throw new BusinessException(404, "策略定义不存在");
        }

        // 权限校验：只能删除自己的策略
        if (!strategy.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该策略");
        }

        // TODO: 检查是否有关联的参数、交易对、订阅等，如果有则不允许删除或级联删除
        // 这里先简单实现物理删除
        removeById(strategyId);
        
        logger.info("策略定义删除成功: strategyId={}", strategyId);
    }

    @Override
    public StrategyDefinition getStrategyById(Long strategyId, Long userId) {
        logger.debug("查询策略定义详情: strategyId={}, userId={}", strategyId, userId);

        StrategyDefinition strategy = getById(strategyId);
        if (strategy == null) {
            throw new BusinessException(404, "策略定义不存在");
        }

        // 权限校验
        if (!strategy.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权访问该策略");
        }

        return strategy;
    }

    @Autowired
    @Lazy
    private StrategyParamService strategyParamService;

    @Autowired
    @Lazy
    private StrategySymbolService strategySymbolService;

    @Autowired
    @Lazy
    private StrategySignalSubscriptionService strategySignalSubscriptionService;

    @Autowired
    @Lazy
    private TradingPairService tradingPairService;

    @Autowired
    @Lazy
    private SignalConfigService signalConfigService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StrategyDefinition createStrategyComplete(Long userId, StrategyCreateRequest request) {
        StrategyCreateRequest.StrategyDefinitionPart definition = request.getDefinition();
        logger.info("完整创建策略（事务性）: userId={}, strategyName={}", userId, definition.getStrategyName());

        try {
            // 1. 创建策略定义
            StrategyDefinition strategy = createStrategy(
                    userId,
                    definition.getStrategyName(),
                    definition.getStrategyType(),
                    definition.getDecisionMode(),
                    definition.getEnabled() != null ? definition.getEnabled() : EnabledStatus.ENABLED
            );
            Long strategyId = strategy.getId();

            // 2. 创建策略参数
            StrategyCreateRequest.StrategyParamPart param = request.getParam();
            strategyParamService.saveOrUpdateParam(
                    userId,
                    strategyId,
                    param.getInitialCapital(),
                    param.getBaseOrderRatio(),
                    param.getTakeProfitRatio(),
                    param.getStopLossRatio(),
                    param.getEntryCondition(),
                    param.getExitCondition()
            );

            // 3. 创建交易对配置
            if (request.getTradingPairs() != null && !request.getTradingPairs().isEmpty()) {
                for (StrategyCreateRequest.TradingPairPart pairPart : request.getTradingPairs()) {
                    strategySymbolService.createStrategySymbol(
                            userId,
                            strategyId,
                            pairPart.getTradingPairId(),
                            pairPart.getEnabled() != null ? pairPart.getEnabled() : EnabledStatus.ENABLED
                    );
                }
            }

            // 4. 创建信号订阅配置
            if (request.getSignalSubscriptions() != null && !request.getSignalSubscriptions().isEmpty()) {
                for (StrategyCreateRequest.SignalSubscriptionPart subPart : request.getSignalSubscriptions()) {
                    strategySignalSubscriptionService.createSubscription(
                            userId,
                            strategyId,
                            subPart.getSignalConfigId(),
                            subPart.getConsumeMode() != null ? subPart.getConsumeMode() : ConsumeMode.LATEST_ONLY,
                            subPart.getEnabled() != null ? subPart.getEnabled() : EnabledStatus.ENABLED
                    );
                }
            }

            logger.info("策略完整创建成功（事务提交）: strategyId={}", strategyId);
            return strategy;

        } catch (Exception e) {
            logger.error("策略创建失败，事务回滚: userId={}, error={}", userId, e.getMessage(), e);
            // 事务会自动回滚，这里重新抛出异常
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StrategyDefinition updateStrategyComplete(Long strategyId, Long userId, StrategyCreateRequest request) {
        StrategyCreateRequest.StrategyDefinitionPart definition = request.getDefinition();
        logger.info("完整更新策略（事务性）: userId={}, strategyId={}, strategyName={}", userId, strategyId, definition.getStrategyName());

        try {
            // 权限校验：确保策略存在且属于当前用户
            StrategyDefinition existingStrategy = getStrategyById(strategyId, userId);
            
            // 检查策略是否启用，启用的策略不允许编辑
            if (existingStrategy.getEnabled() == EnabledStatus.ENABLED) {
                throw new BusinessException(400, "启用中的策略不可编辑，请先禁用策略");
            }

            // 1. 更新策略定义
            StrategyDefinition strategy = updateStrategy(
                    strategyId,
                    userId,
                    definition.getStrategyName(),
                    definition.getStrategyType(),
                    definition.getDecisionMode(),
                    definition.getEnabled() // 保持禁用状态
            );

            // 2. 更新策略参数
            StrategyCreateRequest.StrategyParamPart param = request.getParam();
            strategyParamService.saveOrUpdateParam(
                    userId,
                    strategyId,
                    param.getInitialCapital(),
                    param.getBaseOrderRatio(),
                    param.getTakeProfitRatio(),
                    param.getStopLossRatio(),
                    param.getEntryCondition(),
                    param.getExitCondition()
            );

            // 3. 更新交易对配置
            // 获取现有的交易对配置
            List<StrategySymbol> existingSymbols = strategySymbolService.listByStrategyId(strategyId);
            java.util.Set<Long> existingSymbolIds = existingSymbols.stream()
                    .map(StrategySymbol::getId)
                    .collect(java.util.stream.Collectors.toSet());
            
            java.util.Set<Long> newSymbolIds = new java.util.HashSet<>();
            if (request.getTradingPairs() != null && !request.getTradingPairs().isEmpty()) {
                for (StrategyCreateRequest.TradingPairPart pairPart : request.getTradingPairs()) {
                    if (pairPart.getId() != null) {
                        // 更新现有交易对
                        strategySymbolService.updateStrategySymbol(
                                pairPart.getId(),
                                userId,
                                pairPart.getEnabled() != null ? pairPart.getEnabled() : EnabledStatus.ENABLED
                        );
                        newSymbolIds.add(pairPart.getId());
                    } else {
                        // 创建新交易对
                        strategySymbolService.createStrategySymbol(
                                userId,
                                strategyId,
                                pairPart.getTradingPairId(),
                                pairPart.getEnabled() != null ? pairPart.getEnabled() : EnabledStatus.ENABLED
                        );
                    }
                }
            }
            
            // 删除不在新列表中的交易对
            for (Long existingId : existingSymbolIds) {
                if (!newSymbolIds.contains(existingId)) {
                    strategySymbolService.deleteStrategySymbol(existingId, userId);
                }
            }

            // 4. 更新信号订阅配置
            // 获取现有的信号订阅配置
            List<StrategySignalSubscription> existingSubs = strategySignalSubscriptionService.listByStrategyId(strategyId);
            java.util.Set<Long> existingSubIds = existingSubs.stream()
                    .map(StrategySignalSubscription::getId)
                    .collect(java.util.stream.Collectors.toSet());
            
            java.util.Set<Long> newSubIds = new java.util.HashSet<>();
            if (request.getSignalSubscriptions() != null && !request.getSignalSubscriptions().isEmpty()) {
                for (StrategyCreateRequest.SignalSubscriptionPart subPart : request.getSignalSubscriptions()) {
                    if (subPart.getId() != null) {
                        // 更新现有订阅
                        strategySignalSubscriptionService.updateSubscription(
                                subPart.getId(),
                                userId,
                                subPart.getConsumeMode() != null ? subPart.getConsumeMode() : ConsumeMode.LATEST_ONLY,
                                subPart.getEnabled() != null ? subPart.getEnabled() : EnabledStatus.ENABLED
                        );
                        newSubIds.add(subPart.getId());
                    } else {
                        // 创建新订阅
                        strategySignalSubscriptionService.createSubscription(
                                userId,
                                strategyId,
                                subPart.getSignalConfigId(),
                                subPart.getConsumeMode() != null ? subPart.getConsumeMode() : ConsumeMode.LATEST_ONLY,
                                subPart.getEnabled() != null ? subPart.getEnabled() : EnabledStatus.ENABLED
                        );
                    }
                }
            }
            
            // 删除不在新列表中的订阅
            for (Long existingId : existingSubIds) {
                if (!newSubIds.contains(existingId)) {
                    strategySignalSubscriptionService.deleteSubscription(existingId, userId);
                }
            }

            logger.info("策略完整更新成功（事务提交）: strategyId={}", strategyId);
            return strategy;

        } catch (Exception e) {
            logger.error("策略更新失败，事务回滚: strategyId={}, userId={}, error={}", strategyId, userId, e.getMessage(), e);
            // 事务会自动回滚，这里重新抛出异常
            throw e;
        }
    }

    @Override
    public StrategyDetailVO getStrategyDetail(Long strategyId, Long userId) {
        logger.debug("获取策略完整详情: strategyId={}, userId={}", strategyId, userId);

        // 权限校验：确保策略存在且属于当前用户
        StrategyDefinition strategy = getStrategyById(strategyId, userId);

        // 构建详情VO
        StrategyDetailVO detailVO = new StrategyDetailVO();

        // 1. 策略定义
        StrategyDefinitionVO definitionVO = new StrategyDefinitionVO();
        org.springframework.beans.BeanUtils.copyProperties(strategy, definitionVO);
        detailVO.setDefinition(definitionVO);

        // 2. 策略参数
        StrategyParam param = strategyParamService.getByStrategyId(strategyId);
        if (param != null) {
            StrategyParamVO paramVO = new StrategyParamVO();
            org.springframework.beans.BeanUtils.copyProperties(param, paramVO);
            detailVO.setParam(paramVO);
        }

        // 3. 交易对列表
        List<StrategySymbol> symbols = strategySymbolService.listByStrategyId(strategyId);
        if (symbols != null && !symbols.isEmpty()) {
            // 获取所有交易对ID，批量查询交易对信息
            List<Long> tradingPairIds = symbols.stream()
                    .map(StrategySymbol::getTradingPairId)
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());

            java.util.Map<Long, TradingPair> tradingPairMap =
                    tradingPairService.listByIds(tradingPairIds).stream()
                            .collect(java.util.stream.Collectors.toMap(
                                    TradingPair::getId,
                                    pair -> pair
                            ));

            List<StrategySymbolVO> symbolVOList = symbols.stream()
                    .map(symbol -> {
                        StrategySymbolVO vo = new StrategySymbolVO();
                        org.springframework.beans.BeanUtils.copyProperties(symbol, vo);
                        TradingPair pair = tradingPairMap.get(symbol.getTradingPairId());
                        if (pair != null) {
                            vo.setTradingPairName(pair.getSymbol());
                            vo.setMarketType(pair.getMarketType());
                        }
                        return vo;
                    })
                    .collect(java.util.stream.Collectors.toList());
            detailVO.setTradingPairs(symbolVOList);
        }

        // 4. 信号订阅列表
        List<StrategySignalSubscription> subscriptions =
                strategySignalSubscriptionService.listByStrategyId(strategyId);
        if (subscriptions != null && !subscriptions.isEmpty()) {
            // 获取所有信号配置ID，批量查询信号配置信息
            List<Long> signalConfigIds = subscriptions.stream()
                    .map(StrategySignalSubscription::getSignalConfigId)
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());

            java.util.Map<Long, com.qyl.v2trade.business.signal.model.entity.SignalConfig> signalConfigMap =
                    signalConfigService.listByIds(signalConfigIds).stream()
                            .collect(java.util.stream.Collectors.toMap(
                                    com.qyl.v2trade.business.signal.model.entity.SignalConfig::getId,
                                    config -> config
                            ));

            List<StrategySignalSubscriptionVO> subscriptionVOList = subscriptions.stream()
                    .map(subscription -> {
                        StrategySignalSubscriptionVO vo =
                                new StrategySignalSubscriptionVO();
                        org.springframework.beans.BeanUtils.copyProperties(subscription, vo);
                        com.qyl.v2trade.business.signal.model.entity.SignalConfig config = signalConfigMap.get(subscription.getSignalConfigId());
                        if (config != null) {
                            vo.setSignalConfigName(config.getSignalName());
                        }
                        return vo;
                    })
                    .collect(java.util.stream.Collectors.toList());
            detailVO.setSignalSubscriptions(subscriptionVOList);
        }

        return detailVO;
    }
}

