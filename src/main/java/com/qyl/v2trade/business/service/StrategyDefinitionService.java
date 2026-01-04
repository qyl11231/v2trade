package com.qyl.v2trade.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qyl.v2trade.business.model.entity.StrategyDefinition;

import java.util.List;

/**
 * 策略定义服务接口
 */
public interface StrategyDefinitionService extends IService<StrategyDefinition> {

    /**
     * 根据用户ID查询所有策略定义
     * @param userId 用户ID
     * @return 策略定义列表
     */
    List<StrategyDefinition> listByUserId(Long userId);

    /**
     * 根据用户ID和策略名称查询策略定义
     * @param userId 用户ID
     * @param strategyName 策略名称
     * @return 策略定义，未找到返回null
     */
    StrategyDefinition getByUserIdAndName(Long userId, String strategyName);

    /**
     * 创建策略定义
     * @param userId 用户ID
     * @param strategyName 策略名称
     * @param strategyType 策略类型
     * @param decisionMode 决策模式
     * @param enabled 是否启用
     * @return 创建的策略定义
     */
    StrategyDefinition createStrategy(Long userId, String strategyName, String strategyType, String decisionMode, Integer enabled);

    /**
     * 更新策略定义
     * @param strategyId 策略ID
     * @param userId 用户ID（用于权限校验）
     * @param strategyName 策略名称（可选）
     * @param strategyType 策略类型（可选）
     * @param decisionMode 决策模式（可选）
     * @param enabled 是否启用（可选）
     * @return 更新后的策略定义
     */
    StrategyDefinition updateStrategy(Long strategyId, Long userId, String strategyName, String strategyType, String decisionMode, Integer enabled);

    /**
     * 删除策略定义
     * @param strategyId 策略ID
     * @param userId 用户ID（用于权限校验）
     */
    void deleteStrategy(Long strategyId, Long userId);

    /**
     * 根据ID查询策略定义
     * @param strategyId 策略ID
     * @param userId 用户ID（用于权限校验）
     * @return 策略定义
     */
    StrategyDefinition getStrategyById(Long strategyId, Long userId);

    /**
     * 完整创建策略（事务性，包含定义、参数、交易对、信号订阅）
     * @param userId 用户ID
     * @param request 完整策略创建请求
     * @return 创建的策略定义
     */
    StrategyDefinition createStrategyComplete(Long userId, com.qyl.v2trade.business.model.dto.StrategyCreateRequest request);

    /**
     * 完整更新策略（事务性，包含定义、参数、交易对、信号订阅）
     * @param strategyId 策略ID
     * @param userId 用户ID
     * @param request 完整策略更新请求
     * @return 更新后的策略定义
     */
    StrategyDefinition updateStrategyComplete(Long strategyId, Long userId, com.qyl.v2trade.business.model.dto.StrategyCreateRequest request);

    /**
     * 获取策略完整详情（包含定义、参数、交易对、信号订阅）
     * @param strategyId 策略ID
     * @param userId 用户ID（用于权限校验）
     * @return 策略完整详情
     */
    com.qyl.v2trade.business.model.dto.StrategyDetailVO getStrategyDetail(Long strategyId, Long userId);
}

