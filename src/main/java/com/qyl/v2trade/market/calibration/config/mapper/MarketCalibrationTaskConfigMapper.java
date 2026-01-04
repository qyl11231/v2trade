package com.qyl.v2trade.market.calibration.config.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qyl.v2trade.market.calibration.config.entity.MarketCalibrationTaskConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 行情校准任务配置Mapper接口
 */
@Mapper
public interface MarketCalibrationTaskConfigMapper extends BaseMapper<MarketCalibrationTaskConfig> {
}

