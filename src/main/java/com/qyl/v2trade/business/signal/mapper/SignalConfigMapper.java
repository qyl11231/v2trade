package com.qyl.v2trade.business.signal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qyl.v2trade.business.signal.model.entity.SignalConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 信号配置Mapper接口
 */
@Mapper
public interface SignalConfigMapper extends BaseMapper<SignalConfig> {
}

