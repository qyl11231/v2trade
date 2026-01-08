package com.qyl.v2trade.indicator.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qyl.v2trade.indicator.repository.entity.IndicatorSubscription;
import org.apache.ibatis.annotations.Mapper;

/**
 * 指标订阅Mapper
 */
@Mapper
public interface IndicatorSubscriptionMapper extends BaseMapper<IndicatorSubscription> {
}

