package com.qyl.v2trade.indicator.definition;

import java.util.Set;

/**
 * 指标定义（领域对象）
 * 
 * <p>用于指标注册和元数据管理
 *
 * @author qyl
 */
public interface IndicatorDefinition {
    
    /**
     * 指标编码（如：RSI_14, MACD, SMA_20）
     */
    String code();
    
    /**
     * 指标版本（v1.2.1固定为"v1"）
     */
    String version();
    
    /**
     * 指标名称（展示用）
     */
    String name();
    
    /**
     * 指标分类
     */
    IndicatorCategory category();
    
    /**
     * 计算引擎类型
     */
    String engine();
    
    /**
     * 参数Schema
     */
    ParameterSpec parameters();
    
    /**
     * 返回Schema
     */
    ReturnSpec returns();
    
    /**
     * 最小所需bar数量
     */
    int minRequiredBars();
    
    /**
     * 支持的周期列表
     */
    Set<String> supportedTimeframes();
}

