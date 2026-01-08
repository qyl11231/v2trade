package com.qyl.v2trade.indicator.repository;

import com.qyl.v2trade.indicator.repository.entity.IndicatorDefinition;

import java.util.List;

/**
 * 指标定义Repository
 */
public interface IndicatorDefinitionRepository {
    
    /**
     * 保存系统指标定义（user_id=0，重复忽略）
     * 
     * @param defs 指标定义列表
     */
    void saveSystemDefinitions(List<IndicatorDefinition> defs);
    
    /**
     * 查询启用的指标定义
     * 
     * @param userId 用户ID（0=系统内置，>0=租户）
     * @return 启用的指标定义列表
     */
    List<IndicatorDefinition> listEnabled(long userId);
    
    /**
     * 分页查询指标定义（用于前端API）
     * 
     * @param keyword 关键字（code/name搜索）
     * @param category 分类（MOMENTUM/TREND/VOLATILITY）
     * @param engine 引擎（ta4j/custom）
     * @param enabled 是否启用（1/0）
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @return 分页结果
     */
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<IndicatorDefinition> 
            queryWithPagination(String keyword, String category, String engine, Integer enabled, int page, int size);
}

