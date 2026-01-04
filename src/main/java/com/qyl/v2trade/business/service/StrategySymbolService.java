package com.qyl.v2trade.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qyl.v2trade.business.model.entity.StrategySymbol;

import java.util.List;

/**
 * 策略交易对服务接口
 */
public interface StrategySymbolService extends IService<StrategySymbol> {

    /**
     * 根据策略ID查询所有交易对
     * @param strategyId 策略ID
     * @return 策略交易对列表
     */
    List<StrategySymbol> listByStrategyId(Long strategyId);

    /**
     * 根据用户ID查询所有策略交易对
     * @param userId 用户ID
     * @return 策略交易对列表
     */
    List<StrategySymbol> listByUserId(Long userId);

    /**
     * 创建策略交易对
     * @param userId 用户ID
     * @param strategyId 策略ID
     * @param tradingPairId 交易对ID
     * @param enabled 是否启用
     * @return 创建的策略交易对
     */
    StrategySymbol createStrategySymbol(Long userId, Long strategyId, Long tradingPairId, Integer enabled);

    /**
     * 更新策略交易对
     * @param id 主键ID
     * @param userId 用户ID（用于权限校验）
     * @param enabled 是否启用
     * @return 更新后的策略交易对
     */
    StrategySymbol updateStrategySymbol(Long id, Long userId, Integer enabled);

    /**
     * 删除策略交易对
     * @param id 主键ID
     * @param userId 用户ID（用于权限校验）
     */
    void deleteStrategySymbol(Long id, Long userId);
}

