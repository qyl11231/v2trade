package com.qyl.v2trade.indicator.repository;

import com.qyl.v2trade.indicator.repository.entity.IndicatorCalcLog;

/**
 * 指标计算日志Repository
 */
public interface IndicatorCalcLogRepository {
    
    /**
     * 追加写入计算日志（不更新）
     * 
     * @param log 计算日志
     * @return 是否成功
     */
    boolean append(IndicatorCalcLog log);
    
    /**
     * 分页查询计算日志（用于前端API）
     * 
     * @param userId 用户ID（可选）
     * @param tradingPairId 交易对ID（可选）
     * @param timeframe 周期（可选）
     * @param indicatorCode 指标编码（可选）
     * @param status 状态（可选：SUCCESS/FAILED/CONFLICT）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @param page 页码
     * @param size 每页大小
     * @return 分页结果
     */
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<IndicatorCalcLog> 
            queryWithPagination(Long userId, Long tradingPairId, String timeframe, 
                              String indicatorCode, String status,
                              java.time.LocalDateTime startTime, java.time.LocalDateTime endTime,
                              int page, int size);
}

