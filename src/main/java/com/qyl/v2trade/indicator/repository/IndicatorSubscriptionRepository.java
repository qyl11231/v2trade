package com.qyl.v2trade.indicator.repository;

import com.qyl.v2trade.indicator.repository.entity.IndicatorSubscription;

import java.util.List;

/**
 * 指标订阅Repository
 */
public interface IndicatorSubscriptionRepository {
    
    /**
     * 查询用户的所有启用订阅
     * 
     * @param userId 用户ID
     * @return 订阅列表
     */
    List<IndicatorSubscription> listEnabledByUser(long userId);
    
    /**
     * 查询指定交易对和周期的启用订阅
     * 
     * @param userId 用户ID
     * @param pairId 交易对ID
     * @param timeframe 周期
     * @return 订阅列表
     */
    List<IndicatorSubscription> listEnabledByPairTf(long userId, long pairId, String timeframe);
    
    /**
     * 查询指定交易对和周期的所有启用订阅（不指定userId，用于BarClosedEvent处理）
     * 
     * @param pairId 交易对ID
     * @param timeframe 周期
     * @return 订阅列表
     */
    List<IndicatorSubscription> findByPairAndTimeframe(long pairId, String timeframe);
    
    /**
     * 查询指定用户、交易对和周期的启用订阅
     * 
     * @param userId 用户ID
     * @param pairId 交易对ID
     * @param timeframe 周期
     * @return 订阅列表
     */
    List<IndicatorSubscription> findByUserAndPairAndTimeframe(long userId, long pairId, String timeframe);
    
    /**
     * Upsert订阅
     * 
     * @param sub 订阅
     */
    void upsert(IndicatorSubscription sub);
    
    /**
     * 分页查询订阅列表（用于前端API）
     * 
     * @param userId 用户ID（可选）
     * @param tradingPairId 交易对ID（可选）
     * @param symbolKeyword symbol关键字（可选，模糊搜索）
     * @param timeframe 周期（可选）
     * @param indicatorCode 指标编码（可选）
     * @param enabled 是否启用（可选，1/0）
     * @param page 页码
     * @param size 每页大小
     * @return 分页结果
     */
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<IndicatorSubscription> 
            queryWithPagination(Long userId, Long tradingPairId, String symbolKeyword, 
                              String timeframe, String indicatorCode, Integer enabled, int page, int size);
    
    /**
     * 根据ID查询订阅
     * 
     * @param id 订阅ID
     * @return 订阅，不存在返回null
     */
    IndicatorSubscription findById(Long id);
}

