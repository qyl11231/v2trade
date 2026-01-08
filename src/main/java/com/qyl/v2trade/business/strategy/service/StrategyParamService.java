package com.qyl.v2trade.business.strategy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qyl.v2trade.business.strategy.model.entity.StrategyParam;

import java.util.List;

/**
 * 策略参数服务接口
 */
public interface StrategyParamService extends IService<StrategyParam> {

    /**
     * 根据策略ID查询参数
     * @param strategyId 策略ID
     * @return 策略参数，未找到返回null
     */
    StrategyParam getByStrategyId(Long strategyId);

    /**
     * 根据用户ID查询所有策略参数
     * @param userId 用户ID
     * @return 策略参数列表
     */
    List<StrategyParam> listByUserId(Long userId);

    /**
     * 创建或更新策略参数（策略ID唯一）
     * @param userId 用户ID
     * @param strategyId 策略ID
     * @param initialCapital 初始资金
     * @param baseOrderRatio 单次下单比例
     * @param takeProfitRatio 止盈比例
     * @param stopLossRatio 止损比例
     * @param entryCondition 入场条件JSON
     * @param exitCondition 退出条件JSON
     * @return 策略参数
     */
    StrategyParam saveOrUpdateParam(Long userId, Long strategyId, java.math.BigDecimal initialCapital, 
                                    java.math.BigDecimal baseOrderRatio, java.math.BigDecimal takeProfitRatio, 
                                    java.math.BigDecimal stopLossRatio, String entryCondition, String exitCondition);

    /**
     * 删除策略参数
     * @param strategyId 策略ID
     * @param userId 用户ID（用于权限校验）
     */
    void deleteParam(Long strategyId, Long userId);
}

