package com.qyl.v2trade.business.strategy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qyl.v2trade.business.strategy.model.entity.StrategyLogicState;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 策略逻辑状态 Mapper
 * 
 * <p>用于持久化策略运行状态快照
 *
 * @author qyl
 */
@Mapper
public interface StrategyLogicStateMapper extends BaseMapper<StrategyLogicState> {
    
    /**
     * 根据实例ID查询状态（用于恢复）
     * 
     * @param instanceId 策略实例ID
     * @return 状态记录，未找到返回 null
     */
    StrategyLogicState selectByInstanceId(@Param("instanceId") Long instanceId);
    
    /**
     * 批量查询多个实例的状态（用于启动装载）
     * 
     * @param instanceIds 策略实例ID列表
     * @return 状态记录列表
     */
    List<StrategyLogicState> selectByInstanceIds(@Param("instanceIds") List<Long> instanceIds);
    
    /**
     * 插入或更新状态（UPSERT，按 instanceId 唯一）
     * 
     * <p>注意：需要数据库有唯一约束：UNIQUE(strategy_instance_id)
     * 
     * @param state 状态记录
     * @return 影响行数
     */
    int upsertState(StrategyLogicState state);
}

