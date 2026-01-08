package com.qyl.v2trade.indicator.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qyl.v2trade.indicator.repository.entity.IndicatorCalcLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 指标计算日志Mapper
 */
@Mapper
public interface IndicatorCalcLogMapper extends BaseMapper<IndicatorCalcLog> {
}

