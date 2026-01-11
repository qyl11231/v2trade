package com.qyl.v2trade.business.strategy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qyl.v2trade.business.strategy.model.entity.StrategyIntentRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 策略决策记录Mapper接口
 * 
 * <p>职责：
 * <ul>
 *   <li>提供决策记录的数据库访问接口</li>
 *   <li>阶段2只INSERT，不提供UPDATE/DELETE方法</li>
 * </ul>
 * 
 * <p>阶段2约束：
 * <ul>
 *   <li>只INSERT，不允许UPDATE/DELETE</li>
 *   <li>查询方法用于回放和审计</li>
 * </ul>
 */
@Mapper
public interface StrategyIntentRecordMapper extends BaseMapper<StrategyIntentRecord> {

    /**
     * 根据策略ID和交易对ID查询决策记录（按时间倒序）
     * 
     * @param strategyId 策略ID
     * @param tradingPairId 交易对ID
     * @return 决策记录列表
     */
    List<StrategyIntentRecord> selectByStrategyAndPair(
            @Param("strategyId") Long strategyId,
            @Param("tradingPairId") Long tradingPairId
    );

    /**
     * 根据策略ID和交易对ID查询指定时间范围内的决策记录
     * 
     * <p>用于回放和审计
     * 
     * @param strategyId 策略ID
     * @param tradingPairId 交易对ID
     * @param startTime 开始时间（包含）
     * @param endTime 结束时间（包含）
     * @return 决策记录列表
     */
    List<StrategyIntentRecord> selectByStrategyAndPairAndTimeRange(
            @Param("strategyId") Long strategyId,
            @Param("tradingPairId") Long tradingPairId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 根据策略ID查询所有交易对的决策记录（按时间倒序）
     * 
     * @param strategyId 策略ID
     * @param limit 限制数量
     * @return 决策记录列表
     */
    List<StrategyIntentRecord> selectByStrategyId(
            @Param("strategyId") Long strategyId,
            @Param("limit") Integer limit
    );
}

