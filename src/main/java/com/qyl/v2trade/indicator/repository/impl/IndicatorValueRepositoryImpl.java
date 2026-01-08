package com.qyl.v2trade.indicator.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qyl.v2trade.indicator.repository.IndicatorValueRepository;
import com.qyl.v2trade.indicator.repository.entity.IndicatorValue;
import com.qyl.v2trade.indicator.repository.mapper.IndicatorValueMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 指标值Repository实现
 */
@Slf4j
@Repository
public class IndicatorValueRepositoryImpl implements IndicatorValueRepository {
    
    @Autowired
    private IndicatorValueMapper mapper;
    
    @Override
    public WriteResult insertIgnore(IndicatorValue value) {
        if (value == null) {
            throw new IllegalArgumentException("指标值不能为null");
        }
        
        try {
            // 使用INSERT IGNORE语法
            // MyBatis-Plus不直接支持INSERT IGNORE，使用自定义SQL
            // 这里先尝试查询是否存在
            Optional<IndicatorValue> existing = findOneKey(
                    value.getUserId(),
                    value.getTradingPairId(),
                    value.getTimeframe(),
                    value.getBarTime(),
                    value.getIndicatorCode(),
                    value.getIndicatorVersion()
            );
            
            if (existing.isPresent()) {
                log.debug("指标值已存在，跳过插入: userId={}, pairId={}, timeframe={}, barTime={}, code={}",
                        value.getUserId(), value.getTradingPairId(), value.getTimeframe(),
                        value.getBarTime(), value.getIndicatorCode());
                return WriteResult.IGNORED;
            }
            
            // 插入新记录
            int result = mapper.insert(value);
            if (result > 0) {
                log.debug("插入指标值成功: userId={}, pairId={}, timeframe={}, barTime={}, code={}",
                        value.getUserId(), value.getTradingPairId(), value.getTimeframe(),
                        value.getBarTime(), value.getIndicatorCode());
                return WriteResult.INSERTED;
            } else {
                log.warn("插入指标值失败: userId={}, pairId={}, timeframe={}, barTime={}, code={}",
                        value.getUserId(), value.getTradingPairId(), value.getTimeframe(),
                        value.getBarTime(), value.getIndicatorCode());
                return WriteResult.IGNORED;
            }
            
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 唯一键冲突（并发场景）
            log.debug("指标值已存在（并发冲突），跳过插入: userId={}, pairId={}, timeframe={}, barTime={}, code={}",
                    value.getUserId(), value.getTradingPairId(), value.getTimeframe(),
                    value.getBarTime(), value.getIndicatorCode());
            return WriteResult.IGNORED;
        } catch (Exception e) {
            log.error("插入指标值异常: userId={}, pairId={}, timeframe={}, barTime={}, code={}",
                    value.getUserId(), value.getTradingPairId(), value.getTimeframe(),
                    value.getBarTime(), value.getIndicatorCode(), e);
            throw new RuntimeException("插入指标值失败", e);
        }
    }
    
    @Override
    public Optional<IndicatorValue> findOneKey(long userId, long tradingPairId, String timeframe,
                                                LocalDateTime barTime, String indicatorCode, String indicatorVersion) {
        IndicatorValue value = mapper.selectOne(
                new LambdaQueryWrapper<IndicatorValue>()
                        .eq(IndicatorValue::getUserId, userId)
                        .eq(IndicatorValue::getTradingPairId, tradingPairId)
                        .eq(IndicatorValue::getTimeframe, timeframe)
                        .eq(IndicatorValue::getBarTime, barTime)
                        .eq(IndicatorValue::getIndicatorCode, indicatorCode)
                        .eq(IndicatorValue::getIndicatorVersion, indicatorVersion)
        );
        
        return Optional.ofNullable(value);
    }
    
    @Override
    public Optional<IndicatorValue> findLatest(long userId, long tradingPairId, String timeframe, 
                                               String indicatorCode, String indicatorVersion) {
        IndicatorValue value = mapper.selectOne(
                new LambdaQueryWrapper<IndicatorValue>()
                        .eq(IndicatorValue::getUserId, userId)
                        .eq(IndicatorValue::getTradingPairId, tradingPairId)
                        .eq(IndicatorValue::getTimeframe, timeframe)
                        .eq(IndicatorValue::getIndicatorCode, indicatorCode)
                        .eq(IndicatorValue::getIndicatorVersion, indicatorVersion)
                        .orderByDesc(IndicatorValue::getBarTime)
                        .last("LIMIT 1")
        );
        
        return Optional.ofNullable(value);
    }
    
    @Override
    public Page<IndicatorValue> queryWithPagination(
            Long userId, Long tradingPairId, String timeframe, 
            String indicatorCode, String indicatorVersion,
            LocalDateTime startTime, LocalDateTime endTime,
            int page, int size) {
        
        LambdaQueryWrapper<IndicatorValue> wrapper = new LambdaQueryWrapper<>();
        
        if (userId != null) {
            wrapper.eq(IndicatorValue::getUserId, userId);
        }
        
        if (tradingPairId != null) {
            wrapper.eq(IndicatorValue::getTradingPairId, tradingPairId);
        }
        
        if (StringUtils.hasText(timeframe)) {
            wrapper.eq(IndicatorValue::getTimeframe, timeframe);
        }
        
        if (StringUtils.hasText(indicatorCode)) {
            wrapper.eq(IndicatorValue::getIndicatorCode, indicatorCode);
        }
        
        if (StringUtils.hasText(indicatorVersion)) {
            wrapper.eq(IndicatorValue::getIndicatorVersion, indicatorVersion);
        }
        
        if (startTime != null) {
            wrapper.ge(IndicatorValue::getBarTime, startTime);
        }
        
        if (endTime != null) {
            wrapper.le(IndicatorValue::getBarTime, endTime);
        }
        
        wrapper.orderByDesc(IndicatorValue::getBarTime);
        
        Page<IndicatorValue> pageObj = new Page<>(page, size);
        return mapper.selectPage(pageObj, wrapper);
    }
}
