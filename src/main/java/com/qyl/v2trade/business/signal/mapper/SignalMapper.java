package com.qyl.v2trade.business.signal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qyl.v2trade.business.signal.model.entity.Signal;
import org.apache.ibatis.annotations.Mapper;

/**
 * 信号Mapper接口
 */
@Mapper
public interface SignalMapper extends BaseMapper<Signal> {
}

