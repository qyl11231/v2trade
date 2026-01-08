package com.qyl.v2trade.indicator.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qyl.v2trade.indicator.repository.IndicatorCalcLogRepository;
import com.qyl.v2trade.indicator.repository.entity.IndicatorCalcLog;
import com.qyl.v2trade.indicator.repository.mapper.IndicatorCalcLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 指标计算日志Repository实现
 */
@Slf4j
@Repository
public class IndicatorCalcLogRepositoryImpl implements IndicatorCalcLogRepository {
    
    @Autowired
    private IndicatorCalcLogMapper mapper;
    
    @Override
    public boolean append(IndicatorCalcLog log) {
        if (log == null) {
            throw new IllegalArgumentException("计算日志不能为null");
        }
        
        try {
            int result = mapper.insert(log);
            if (result > 0) {
                IndicatorCalcLogRepositoryImpl.log.debug("写入计算日志成功: userId={}, pairId={}, timeframe={}, barTime={}, code={}, status={}",
                        log.getUserId(), log.getTradingPairId(), log.getTimeframe(),
                        log.getBarTime(), log.getIndicatorCode(), log.getStatus());
                return true;
            } else {
                IndicatorCalcLogRepositoryImpl.log.warn("写入计算日志失败: userId={}, pairId={}, timeframe={}, barTime={}, code={}",
                        log.getUserId(), log.getTradingPairId(), log.getTimeframe(),
                        log.getBarTime(), log.getIndicatorCode());
                return false;
            }
        } catch (Exception e) {
            IndicatorCalcLogRepositoryImpl.log.error("写入计算日志异常: userId={}, pairId={}, timeframe={}, barTime={}, code={}",
                    log.getUserId(), log.getTradingPairId(), log.getTimeframe(),
                    log.getBarTime(), log.getIndicatorCode(), e);
            return false;
        }
    }
    
    @Override
    public Page<IndicatorCalcLog> queryWithPagination(
            Long userId, Long tradingPairId, String timeframe, 
            String indicatorCode, String status,
            LocalDateTime startTime, LocalDateTime endTime,
            int page, int size) {
        
        LambdaQueryWrapper<IndicatorCalcLog> wrapper = new LambdaQueryWrapper<>();
        
        if (userId != null) {
            wrapper.eq(IndicatorCalcLog::getUserId, userId);
        }
        
        if (tradingPairId != null) {
            wrapper.eq(IndicatorCalcLog::getTradingPairId, tradingPairId);
        }
        
        if (StringUtils.hasText(timeframe)) {
            wrapper.eq(IndicatorCalcLog::getTimeframe, timeframe);
        }
        
        if (StringUtils.hasText(indicatorCode)) {
            wrapper.eq(IndicatorCalcLog::getIndicatorCode, indicatorCode);
        }
        
        if (StringUtils.hasText(status)) {
            wrapper.eq(IndicatorCalcLog::getStatus, status);
        }
        
        if (startTime != null) {
            wrapper.ge(IndicatorCalcLog::getCreatedAt, startTime);
        }
        
        if (endTime != null) {
            wrapper.le(IndicatorCalcLog::getCreatedAt, endTime);
        }
        
        wrapper.orderByDesc(IndicatorCalcLog::getCreatedAt);
        
        Page<IndicatorCalcLog> pageObj = new Page<>(page, size);
        return mapper.selectPage(pageObj, wrapper);
    }
}
