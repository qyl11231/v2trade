package com.qyl.v2trade.business.strategy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qyl.v2trade.business.strategy.model.entity.StrategyInstance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 策略实例Mapper
 */
@Mapper
public interface StrategyInstanceMapper extends BaseMapper<StrategyInstance> {
    
    /**
     * 根据策略ID查询所有实例
     * @param strategyId 策略ID
     * @return 实例列表
     */
    List<StrategyInstance> selectByStrategyId(@Param("strategyId") Long strategyId);
    
    /**
     * 根据用户ID和策略ID查询实例列表
     * @param userId 用户ID
     * @param strategyId 策略ID
     * @return 实例列表
     */
    List<StrategyInstance> selectByUserIdAndStrategyId(@Param("userId") Long userId, 
                                                        @Param("strategyId") Long strategyId);
    
    /**
     * 唯一性校验：同一用户、同一策略、同一交易对、同一信号配置 = 唯一实例
     * @param userId 用户ID
     * @param strategyId 策略ID
     * @param tradingPairId 交易对ID
     * @param signalConfigId 信号配置ID（0表示无信号绑定）
     * @return 实例，未找到返回null
     */
    StrategyInstance selectByUniqueKey(@Param("userId") Long userId,
                                       @Param("strategyId") Long strategyId,
                                       @Param("tradingPairId") Long tradingPairId,
                                       @Param("signalConfigId") Long signalConfigId);
    
    /**
     * 根据交易对ID查询启用的实例
     * @param tradingPairId 交易对ID
     * @return 启用的实例列表
     */
    List<StrategyInstance> selectEnabledByTradingPairId(@Param("tradingPairId") Long tradingPairId);
    
    /**
     * 根据信号配置ID查询启用的实例
     * @param signalConfigId 信号配置ID
     * @return 启用的实例列表
     */
    List<StrategyInstance> selectEnabledBySignalConfigId(@Param("signalConfigId") Long signalConfigId);
    
    /**
     * 查询所有启用的实例（用于启动预热索引）
     * @return 所有启用的实例列表
     */
    List<StrategyInstance> selectAllEnabled();
}

