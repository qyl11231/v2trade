package com.qyl.v2trade.business.strategy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qyl.v2trade.business.strategy.model.entity.SignalIntent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 信号意图Mapper接口
 * 
 * <p>职责：
 * <ul>
 *   <li>提供信号意图的查询方法</li>
 *   <li>用于AtomicSampler读取LATEST_ONLY信号</li>
 * </ul>
 */
@Mapper
public interface SignalIntentMapper extends BaseMapper<SignalIntent> {
    // 所有查询方法都通过 BaseMapper 和 LambdaQueryWrapper 实现
}

