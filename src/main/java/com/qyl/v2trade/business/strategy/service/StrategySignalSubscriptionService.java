package com.qyl.v2trade.business.strategy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qyl.v2trade.business.strategy.model.entity.StrategySignalSubscription;

import java.util.List;

/**
 * 策略信号订阅服务接口
 */
public interface StrategySignalSubscriptionService extends IService<StrategySignalSubscription> {

    /**
     * 根据策略ID查询所有订阅
     * @param strategyId 策略ID
     * @return 策略信号订阅列表
     */
    List<StrategySignalSubscription> listByStrategyId(Long strategyId);

    /**
     * 根据用户ID查询所有策略信号订阅
     * @param userId 用户ID
     * @return 策略信号订阅列表
     */
    List<StrategySignalSubscription> listByUserId(Long userId);

    /**
     * 创建策略信号订阅
     * @param userId 用户ID
     * @param strategyId 策略ID
     * @param signalConfigId 信号配置ID
     * @param consumeMode 消费模式
     * @param enabled 是否启用
     * @return 创建的策略信号订阅
     */
    StrategySignalSubscription createSubscription(Long userId, Long strategyId, Long signalConfigId, String consumeMode, Integer enabled);

    /**
     * 更新策略信号订阅
     * @param id 订阅ID
     * @param userId 用户ID（用于权限校验）
     * @param consumeMode 消费模式（可选）
     * @param enabled 是否启用（可选）
     * @return 更新后的策略信号订阅
     */
    StrategySignalSubscription updateSubscription(Long id, Long userId, String consumeMode, Integer enabled);

    /**
     * 删除策略信号订阅
     * @param id 订阅ID
     * @param userId 用户ID（用于权限校验）
     */
    void deleteSubscription(Long id, Long userId);
}

