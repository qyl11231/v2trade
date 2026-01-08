package com.qyl.v2trade.business.strategy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qyl.v2trade.business.strategy.model.entity.StrategyDefinition;
import org.apache.ibatis.annotations.Mapper;

/**
 * 策略定义Mapper接口
 */
@Mapper
public interface StrategyDefinitionMapper extends BaseMapper<StrategyDefinition> {
}

