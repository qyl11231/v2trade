package com.qyl.v2trade.indicator.repository;

import com.qyl.v2trade.indicator.repository.entity.IndicatorDefinition;

import java.util.List;
import java.util.Optional;

/**
 * 指标定义Repository（V2：单表设计）
 * 
 * <p>【V2 核心变化】
 * - 只依赖 indicator_definition 一张表
 * - 支持按 data_source、impl_key 查询
 * - 支持按 code+version 精确查询
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
    
    // ========== V2 新增方法 ==========
    
    /**
     * 根据指标编码和版本查询指标定义（V2新增）
     * 
     * @param indicatorCode 指标编码
     * @param indicatorVersion 指标版本
     * @return 指标定义，不存在返回 Optional.empty()
     */
    Optional<IndicatorDefinition> findByCodeAndVersion(String indicatorCode, String indicatorVersion);
    
    /**
     * 根据实现映射键查询指标定义（V2新增）
     * 
     * @param implKey 实现映射键（如 "ta4j:rsi"）
     * @return 指标定义，不存在返回 Optional.empty()
     */
    Optional<IndicatorDefinition> findByImplKey(String implKey);
    
    /**
     * 根据数据源查询指标定义列表（V2新增）
     * 
     * @param dataSource 数据源（BAR/TICK/SIGNAL/MIXED）
     * @return 指标定义列表
     */
    List<IndicatorDefinition> findByDataSource(String dataSource);
    
    /**
     * 保存或更新指标定义（V2新增，用于CRUD）
     * 
     * @param definition 指标定义
     * @return 保存后的指标定义（包含生成的ID）
     */
    IndicatorDefinition saveOrUpdate(IndicatorDefinition definition);
    
    /**
     * 根据ID查询指标定义（V2新增）
     * 
     * @param id 指标定义ID
     * @return 指标定义，不存在返回 Optional.empty()
     */
    Optional<IndicatorDefinition> findById(Long id);
    
    /**
     * 删除指标定义（V2新增）
     * 
     * @param id 指标定义ID
     * @return 是否删除成功
     */
    boolean deleteById(Long id);
}

