package com.qyl.v2trade.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qyl.v2trade.business.model.entity.MarketSubscriptionConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 行情订阅配置Mapper
 */
@Mapper
public interface MarketSubscriptionConfigMapper extends BaseMapper<MarketSubscriptionConfig> {
}

