package com.qyl.v2trade.business.strategy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qyl.v2trade.business.strategy.model.entity.StrategyLogicState;
import org.apache.ibatis.annotations.Mapper;

/**
 * 策略逻辑状态Mapper接口
 * 
 * <p>职责：
 * <ul>
 *   <li>提供策略逻辑状态的查询和更新方法</li>
 *   <li>核心查询：按策略ID+交易对ID查询（唯一查询）</li>
 * </ul>
 * 
 * <p>注意：使用 MyBatis-Plus 的 BaseMapper 和 LambdaQueryWrapper 进行查询
 */
@Mapper
public interface StrategyLogicStateMapper extends BaseMapper<StrategyLogicState> {
    // 所有查询方法都通过 BaseMapper 和 LambdaQueryWrapper 实现
    // 在 LogicStateRestorer 中使用 selectOne 配合 LambdaQueryWrapper 查询
}

