package com.qyl.v2trade.indicator.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qyl.v2trade.indicator.repository.IndicatorDefinitionRepository;
import com.qyl.v2trade.indicator.repository.entity.IndicatorDefinition;
import com.qyl.v2trade.indicator.repository.mapper.IndicatorDefinitionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 指标定义Repository实现
 */
@Slf4j
@Repository
public class IndicatorDefinitionRepositoryImpl implements IndicatorDefinitionRepository {
    
    @Autowired
    private IndicatorDefinitionMapper mapper;
    
    @Override
    public void saveSystemDefinitions(List<IndicatorDefinition> defs) {
        if (defs == null || defs.isEmpty()) {
            return;
        }
        
        for (IndicatorDefinition def : defs) {
            // 确保user_id=0
            if (def.getUserId() == null || def.getUserId() != 0L) {
                def.setUserId(0L);
            }
            
            // 检查是否已存在
            IndicatorDefinition existing = mapper.selectOne(
                new LambdaQueryWrapper<IndicatorDefinition>()
                    .eq(IndicatorDefinition::getUserId, def.getUserId())
                    .eq(IndicatorDefinition::getIndicatorCode, def.getIndicatorCode())
                    .eq(IndicatorDefinition::getIndicatorVersion, def.getIndicatorVersion())
            );
            
            if (existing != null) {
                log.debug("指标定义已存在，跳过: userId={}, code={}, version={}",
                        def.getUserId(), def.getIndicatorCode(), def.getIndicatorVersion());
                continue;
            }
            
            // 插入新记录
            int result = mapper.insert(def);
            log.debug("插入指标定义: userId={}, code={}, version={}, result={}",
                    def.getUserId(), def.getIndicatorCode(), def.getIndicatorVersion(), result);
        }
    }
    
    @Override
    public List<IndicatorDefinition> listEnabled(long userId) {
        return mapper.selectList(
            new LambdaQueryWrapper<IndicatorDefinition>()
                .eq(IndicatorDefinition::getUserId, userId)
                .eq(IndicatorDefinition::getEnabled, 1)
        );
    }
    
    @Override
    public Page<IndicatorDefinition> queryWithPagination(
            String keyword, String category, String engine, Integer enabled, int page, int size) {
        
        LambdaQueryWrapper<IndicatorDefinition> wrapper = new LambdaQueryWrapper<>();
        
        // keyword搜索（code或name包含）
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(IndicatorDefinition::getIndicatorCode, keyword)
                    .or()
                    .like(IndicatorDefinition::getIndicatorName, keyword));
        }
        
        // category筛选
        if (StringUtils.hasText(category)) {
            wrapper.eq(IndicatorDefinition::getCategory, category);
        }
        
        // engine筛选
        if (StringUtils.hasText(engine)) {
            wrapper.eq(IndicatorDefinition::getEngine, engine);
        }
        
        // enabled筛选
        if (enabled != null) {
            wrapper.eq(IndicatorDefinition::getEnabled, enabled);
        }
        
        // 按updated_at倒序
        wrapper.orderByDesc(IndicatorDefinition::getUpdatedAt);
        
        // 分页查询
        Page<IndicatorDefinition> pageObj = new Page<>(page, size);
        return mapper.selectPage(pageObj, wrapper);
    }
}

