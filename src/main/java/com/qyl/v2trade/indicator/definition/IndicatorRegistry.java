package com.qyl.v2trade.indicator.definition;

import java.util.List;

/**
 * 指标注册表
 * 
 * <p>管理系统中所有可用的指标定义
 *
 * @author qyl
 */
public interface IndicatorRegistry {
    
    /**
     * 注册指标定义
     * 
     * @param definition 指标定义
     */
    void register(IndicatorDefinition definition);
    
    /**
     * 获取指标定义
     * 
     * @param code 指标编码
     * @param version 版本
     * @return 指标定义，如果不存在返回null
     */
    IndicatorDefinition getDefinition(String code, String version);
    
    /**
     * 获取所有已注册的指标定义
     * 
     * @return 指标定义列表
     */
    List<IndicatorDefinition> getAllDefinitions();
    
    /**
     * 检查指标是否已注册
     * 
     * @param code 指标编码
     * @param version 版本
     * @return 是否已注册
     */
    boolean isRegistered(String code, String version);
}

