package com.qyl.v2trade.business.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qyl.v2trade.business.strategy.mapper.StrategySignalSubscriptionMapper;
import com.qyl.v2trade.business.strategy.model.entity.StrategySignalSubscription;
import com.qyl.v2trade.business.strategy.service.StrategyDefinitionService;
import com.qyl.v2trade.business.strategy.service.StrategySignalSubscriptionService;
import com.qyl.v2trade.common.constants.ConsumeMode;
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
 * 策略信号订阅服务实现类
 */
@Service
public class StrategySignalSubscriptionServiceImpl extends ServiceImpl<StrategySignalSubscriptionMapper, StrategySignalSubscription> implements StrategySignalSubscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(StrategySignalSubscriptionServiceImpl.class);

    @Autowired
    @Lazy
    private StrategyDefinitionService strategyDefinitionService;

    @Override
    public List<StrategySignalSubscription> listByStrategyId(Long strategyId) {
        logger.debug("查询策略信号订阅列表: strategyId={}", strategyId);
        
        return list(new LambdaQueryWrapper<StrategySignalSubscription>()
                .eq(StrategySignalSubscription::getStrategyId, strategyId)
                .orderByDesc(StrategySignalSubscription::getCreatedAt));
    }

    @Override
    public List<StrategySignalSubscription> listByUserId(Long userId) {
        logger.debug("查询用户策略信号订阅列表: userId={}", userId);
        
        return list(new LambdaQueryWrapper<StrategySignalSubscription>()
                .eq(StrategySignalSubscription::getUserId, userId)
                .orderByDesc(StrategySignalSubscription::getCreatedAt));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StrategySignalSubscription createSubscription(Long userId, Long strategyId, Long signalConfigId, String consumeMode, Integer enabled) {
        logger.info("创建策略信号订阅: userId={}, strategyId={}, signalConfigId={}", userId, strategyId, signalConfigId);

        // 校验策略是否存在且属于该用户
        try {
            strategyDefinitionService.getStrategyById(strategyId, userId);
        } catch (BusinessException e) {
            throw new BusinessException(404, "策略不存在或无权限");
        }

        // 校验消费模式
        if (consumeMode == null || consumeMode.isEmpty()) {
            consumeMode = ConsumeMode.LATEST_ONLY;
        } else if (!ConsumeMode.LATEST_ONLY.equals(consumeMode)) {
            throw new BusinessException(400, "消费模式不合法，目前只支持LATEST_ONLY");
        }

        // 检查是否已存在相同的订阅（strategy_id + signal_config_id 唯一）
        StrategySignalSubscription existSubscription = getOne(new LambdaQueryWrapper<StrategySignalSubscription>()
                .eq(StrategySignalSubscription::getStrategyId, strategyId)
                .eq(StrategySignalSubscription::getSignalConfigId, signalConfigId));
        
        if (existSubscription != null) {
            throw new BusinessException(400, "该信号配置已订阅");
        }

        StrategySignalSubscription subscription = new StrategySignalSubscription();
        subscription.setUserId(userId);
        subscription.setStrategyId(strategyId);
        subscription.setSignalConfigId(signalConfigId);
        subscription.setConsumeMode(consumeMode);
        subscription.setEnabled(enabled != null && enabled.equals(EnabledStatus.ENABLED) ? EnabledStatus.ENABLED : EnabledStatus.DISABLED);

        save(subscription);
        
        logger.info("策略信号订阅创建成功: id={}", subscription.getId());
        return subscription;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StrategySignalSubscription updateSubscription(Long id, Long userId, String consumeMode, Integer enabled) {
        logger.info("更新策略信号订阅: id={}, userId={}", id, userId);

        StrategySignalSubscription subscription = getById(id);
        if (subscription == null) {
            throw new BusinessException(404, "策略信号订阅不存在");
        }

        // 权限校验：只能更新自己的订阅
        if (!subscription.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该订阅");
        }

        if (consumeMode != null && !consumeMode.isEmpty()) {
            // 校验消费模式
            if (!ConsumeMode.LATEST_ONLY.equals(consumeMode)) {
                throw new BusinessException(400, "消费模式不合法，目前只支持LATEST_ONLY");
            }
            subscription.setConsumeMode(consumeMode);
        }

        if (enabled != null) {
            subscription.setEnabled(enabled.equals(EnabledStatus.ENABLED) ? EnabledStatus.ENABLED : EnabledStatus.DISABLED);
        }

        updateById(subscription);
        
        logger.info("策略信号订阅更新成功: id={}", id);
        return subscription;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSubscription(Long id, Long userId) {
        logger.info("删除策略信号订阅: id={}, userId={}", id, userId);

        StrategySignalSubscription subscription = getById(id);
        if (subscription == null) {
            throw new BusinessException(404, "策略信号订阅不存在");
        }

        // 权限校验：只能删除自己的订阅
        if (!subscription.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该订阅");
        }

        removeById(id);
        
        logger.info("策略信号订阅删除成功: id={}", id);
    }
}

