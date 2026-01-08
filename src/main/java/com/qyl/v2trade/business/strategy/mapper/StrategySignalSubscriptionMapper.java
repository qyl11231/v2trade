package com.qyl.v2trade.business.strategy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qyl.v2trade.business.strategy.model.entity.StrategySignalSubscription;
import org.apache.ibatis.annotations.Mapper;

/**
 * 策略信号订阅Mapper接口
 */
@Mapper
public interface StrategySignalSubscriptionMapper extends BaseMapper<StrategySignalSubscription> {
}

