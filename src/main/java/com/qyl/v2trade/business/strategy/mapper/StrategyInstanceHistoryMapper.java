package com.qyl.v2trade.business.strategy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qyl.v2trade.business.strategy.model.entity.StrategyInstanceHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 策略实例历史记录Mapper
 */
@Mapper
public interface StrategyInstanceHistoryMapper extends BaseMapper<StrategyInstanceHistory> {
    
    /**
     * 根据实例ID查询历史记录列表（按版本号倒序）
     * @param instanceId 实例ID
     * @return 历史记录列表
     */
    List<StrategyInstanceHistory> selectByInstanceId(@Param("instanceId") Long instanceId);
    
    /**
     * 根据实例ID和版本号查询历史记录
     * @param instanceId 实例ID
     * @param version 版本号
     * @return 历史记录，未找到返回null
     */
    StrategyInstanceHistory selectByInstanceIdAndVersion(@Param("instanceId") Long instanceId, 
                                                          @Param("version") Integer version);
}

