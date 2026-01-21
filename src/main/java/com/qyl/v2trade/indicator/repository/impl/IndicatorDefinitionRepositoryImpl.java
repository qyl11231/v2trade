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
import java.util.Optional;

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
    
    // ========== V2 新增方法实现 ==========
    
    @Override
    public Optional<IndicatorDefinition> findByCodeAndVersion(String indicatorCode, String indicatorVersion) {
        if (indicatorCode == null || indicatorVersion == null) {
            return Optional.empty();
        }
        
        IndicatorDefinition definition = mapper.selectOne(
            new LambdaQueryWrapper<IndicatorDefinition>()
                .eq(IndicatorDefinition::getIndicatorCode, indicatorCode)
                .eq(IndicatorDefinition::getIndicatorVersion, indicatorVersion)
                .last("LIMIT 1")
        );
        
        return Optional.ofNullable(definition);
    }
    
    @Override
    public Optional<IndicatorDefinition> findByImplKey(String implKey) {
        if (implKey == null) {
            return Optional.empty();
        }
        
        // impl_key 现在是独立字段，可以直接查询
        IndicatorDefinition definition = mapper.selectOne(
            new LambdaQueryWrapper<IndicatorDefinition>()
                .eq(IndicatorDefinition::getImplKey, implKey)
                .last("LIMIT 1")
        );
        
        return Optional.ofNullable(definition);
    }
    
    @Override
    public List<IndicatorDefinition> findByDataSource(String dataSource) {
        if (dataSource == null) {
            return List.of();
        }
        
        // data_source 现在是独立字段，可以直接查询
        return mapper.selectList(
            new LambdaQueryWrapper<IndicatorDefinition>()
                .eq(IndicatorDefinition::getDataSource, dataSource)
        );
    }
    
    @Override
    public IndicatorDefinition saveOrUpdate(IndicatorDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("指标定义不能为null");
        }
        
        if (definition.getId() != null) {
            // 更新
            mapper.updateById(definition);
            log.debug("更新指标定义: id={}, code={}, version={}", 
                    definition.getId(), definition.getIndicatorCode(), definition.getIndicatorVersion());
        } else {
            // 插入
            mapper.insert(definition);
            log.debug("插入指标定义: id={}, code={}, version={}", 
                    definition.getId(), definition.getIndicatorCode(), definition.getIndicatorVersion());
        }
        
        return definition;
    }
    
    @Override
    public Optional<IndicatorDefinition> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        
        IndicatorDefinition definition = mapper.selectById(id);
        return Optional.ofNullable(definition);
    }
    
    @Override
    public boolean deleteById(Long id) {
        if (id == null) {
            return false;
        }
        
        int result = mapper.deleteById(id);
        log.debug("删除指标定义: id={}, result={}", id, result);
        return result > 0;
    }
}

