package com.qyl.v2trade.business.strategy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qyl.v2trade.business.strategy.model.entity.StrategyDefinition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 策略定义Mapper
 */
@Mapper
public interface StrategyDefinitionMapper extends BaseMapper<StrategyDefinition> {
    
    /**
     * 根据用户ID和策略名称查询（用于唯一性校验）
     * @param userId 用户ID
     * @param strategyName 策略名称
     * @return 策略定义，未找到返回null
     */
    StrategyDefinition selectByUserIdAndStrategyName(@Param("userId") Long userId, 
                                                      @Param("strategyName") String strategyName);
    
    /**
     * 查询用户的所有策略定义（支持按enabled过滤）
     * @param userId 用户ID
     * @param enabled 是否启用（可选，null表示全部）
     * @return 策略定义列表
     */
    List<StrategyDefinition> selectByUserId(@Param("userId") Long userId, 
                                             @Param("enabled") Integer enabled);
}

