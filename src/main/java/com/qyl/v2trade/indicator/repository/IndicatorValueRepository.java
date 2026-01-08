package com.qyl.v2trade.indicator.repository;

import com.qyl.v2trade.indicator.repository.entity.IndicatorValue;

import java.util.Optional;

/**
 * 指标值Repository
 */
public interface IndicatorValueRepository {
    
    /**
     * 幂等插入指标值（唯一键冲突ignore）
     * 
     * @param value 指标值
     * @return 插入结果：INSERTED（新插入）、IGNORED（已存在）
     */
    WriteResult insertIgnore(IndicatorValue value);
    
    /**
     * 根据唯一键查找指标值（用于冲突检测）
     * 
     * @param userId 用户ID
     * @param tradingPairId 交易对ID
     * @param timeframe 周期
     * @param barTime bar时间
     * @param indicatorCode 指标编码
     * @param indicatorVersion 指标版本
     * @return 指标值，不存在返回Optional.empty()
     */
    Optional<IndicatorValue> findOneKey(long userId, long tradingPairId, String timeframe,
                                       java.time.LocalDateTime barTime, String indicatorCode, String indicatorVersion);
    
    /**
     * 查询最新指标值（按bar_time desc limit 1）
     * 
     * @param userId 用户ID
     * @param tradingPairId 交易对ID
     * @param timeframe 周期
     * @param indicatorCode 指标编码
     * @param indicatorVersion 指标版本（可选，默认v1）
     * @return 最新指标值，不存在返回Optional.empty()
     */
    Optional<IndicatorValue> findLatest(long userId, long tradingPairId, String timeframe, 
                                       String indicatorCode, String indicatorVersion);
    
    /**
     * 分页查询指标值（用于前端API）
     * 
     * @param userId 用户ID（可选）
     * @param tradingPairId 交易对ID（可选）
     * @param timeframe 周期（可选）
     * @param indicatorCode 指标编码（可选）
     * @param indicatorVersion 指标版本（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @param page 页码
     * @param size 每页大小
     * @return 分页结果
     */
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<IndicatorValue> 
            queryWithPagination(Long userId, Long tradingPairId, String timeframe, 
                              String indicatorCode, String indicatorVersion,
                              java.time.LocalDateTime startTime, java.time.LocalDateTime endTime,
                              int page, int size);
    
    enum WriteResult {
        INSERTED,   // 新插入
        IGNORED     // 已存在（ignore）
    }
}

