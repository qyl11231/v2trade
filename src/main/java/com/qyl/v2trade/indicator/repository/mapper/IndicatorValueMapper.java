package com.qyl.v2trade.indicator.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qyl.v2trade.indicator.repository.entity.IndicatorValue;
import org.apache.ibatis.annotations.Mapper;

/**
 * 指标值Mapper
 */
@Mapper
public interface IndicatorValueMapper extends BaseMapper<IndicatorValue> {
}

