package com.qyl.v2trade.indicator.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qyl.v2trade.indicator.repository.entity.IndicatorDefinition;
import org.apache.ibatis.annotations.Mapper;

/**
 * 指标定义Mapper
 */
@Mapper
public interface IndicatorDefinitionMapper extends BaseMapper<IndicatorDefinition> {
}

