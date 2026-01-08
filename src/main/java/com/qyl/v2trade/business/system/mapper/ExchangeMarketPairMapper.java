package com.qyl.v2trade.business.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qyl.v2trade.business.system.model.entity.ExchangeMarketPair;
import org.apache.ibatis.annotations.Mapper;

/**
 * 交易所交易规则Mapper接口
 */
@Mapper
public interface ExchangeMarketPairMapper extends BaseMapper<ExchangeMarketPair> {
}

