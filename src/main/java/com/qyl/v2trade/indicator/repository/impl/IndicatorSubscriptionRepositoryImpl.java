package com.qyl.v2trade.indicator.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qyl.v2trade.indicator.repository.IndicatorSubscriptionRepository;
import com.qyl.v2trade.indicator.repository.entity.IndicatorSubscription;
import com.qyl.v2trade.indicator.repository.mapper.IndicatorSubscriptionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 指标订阅Repository实现
 */
@Slf4j
@Repository
public class IndicatorSubscriptionRepositoryImpl implements IndicatorSubscriptionRepository {
    
    @Autowired
    private IndicatorSubscriptionMapper mapper;
    
    @Override
    public List<IndicatorSubscription> listEnabledByUser(long userId) {
        return mapper.selectList(
            new LambdaQueryWrapper<IndicatorSubscription>()
                .eq(IndicatorSubscription::getUserId, userId)
                .eq(IndicatorSubscription::getEnabled, 1)
        );
    }
    
    @Override
    public List<IndicatorSubscription> listEnabledByPairTf(long userId, long pairId, String timeframe) {
        return mapper.selectList(
            new LambdaQueryWrapper<IndicatorSubscription>()
                .eq(IndicatorSubscription::getUserId, userId)
                .eq(IndicatorSubscription::getTradingPairId, pairId)
                .eq(IndicatorSubscription::getTimeframe, timeframe)
                .eq(IndicatorSubscription::getEnabled, 1)
        );
    }
    
    @Override
    public List<IndicatorSubscription> findByPairAndTimeframe(long pairId, String timeframe) {
        return mapper.selectList(
            new LambdaQueryWrapper<IndicatorSubscription>()
                .eq(IndicatorSubscription::getTradingPairId, pairId)
                .eq(IndicatorSubscription::getTimeframe, timeframe)
                .eq(IndicatorSubscription::getEnabled, 1)
        );
    }
    
    @Override
    public List<IndicatorSubscription> findByUserAndPairAndTimeframe(long userId, long pairId, String timeframe) {
        return listEnabledByPairTf(userId, pairId, timeframe);
    }
    
    @Override
    public void upsert(IndicatorSubscription sub) {
        // 检查是否已存在
        IndicatorSubscription existing = mapper.selectOne(
            new LambdaQueryWrapper<IndicatorSubscription>()
                .eq(IndicatorSubscription::getUserId, sub.getUserId())
                .eq(IndicatorSubscription::getTradingPairId, sub.getTradingPairId())
                .eq(IndicatorSubscription::getTimeframe, sub.getTimeframe())
                .eq(IndicatorSubscription::getIndicatorCode, sub.getIndicatorCode())
                .eq(IndicatorSubscription::getIndicatorVersion, sub.getIndicatorVersion())
        );
        
        if (existing != null) {
            // 更新
            sub.setId(existing.getId());
            mapper.updateById(sub);
            log.debug("更新指标订阅: id={}, userId={}, pairId={}, timeframe={}, code={}",
                    existing.getId(), sub.getUserId(), sub.getTradingPairId(), 
                    sub.getTimeframe(), sub.getIndicatorCode());
        } else {
            // 插入
            mapper.insert(sub);
            log.debug("插入指标订阅: userId={}, pairId={}, timeframe={}, code={}",
                    sub.getUserId(), sub.getTradingPairId(), sub.getTimeframe(), sub.getIndicatorCode());
        }
    }
    
    @Override
    public Page<IndicatorSubscription> queryWithPagination(
            Long userId, Long tradingPairId, String symbolKeyword, 
            String timeframe, String indicatorCode, Integer enabled, int page, int size) {
        
        LambdaQueryWrapper<IndicatorSubscription> wrapper = new LambdaQueryWrapper<>();
        
        if (userId != null) {
            wrapper.eq(IndicatorSubscription::getUserId, userId);
        }
        
        if (tradingPairId != null) {
            wrapper.eq(IndicatorSubscription::getTradingPairId, tradingPairId);
        }
        
        if (StringUtils.hasText(symbolKeyword)) {
            wrapper.like(IndicatorSubscription::getSymbol, symbolKeyword);
        }
        
        if (StringUtils.hasText(timeframe)) {
            wrapper.eq(IndicatorSubscription::getTimeframe, timeframe);
        }
        
        if (StringUtils.hasText(indicatorCode)) {
            wrapper.eq(IndicatorSubscription::getIndicatorCode, indicatorCode);
        }
        
        if (enabled != null) {
            wrapper.eq(IndicatorSubscription::getEnabled, enabled);
        }
        
        wrapper.orderByDesc(IndicatorSubscription::getCreatedAt);
        
        Page<IndicatorSubscription> pageObj = new Page<>(page, size);
        return mapper.selectPage(pageObj, wrapper);
    }
    
    @Override
    public IndicatorSubscription findById(Long id) {
        return mapper.selectById(id);
    }
}

