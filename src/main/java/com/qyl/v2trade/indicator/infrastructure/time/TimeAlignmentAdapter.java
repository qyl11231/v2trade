package com.qyl.v2trade.indicator.infrastructure.time;

import com.qyl.v2trade.indicator.domain.model.NormalizedBar;

/**
 * 时间语义对齐适配器
 * 
 * <p>将不同来源的Bar数据转换为指标模块统一使用的NormalizedBar
 * 
 * <p>【重要】这是指标模块唯一允许处理时间语义的地方
 * 所有上游数据源必须通过此接口归一化后进入指标模块
 *
 * @author qyl
 */
public interface TimeAlignmentAdapter {
    
    /**
     * 将上游Bar转换为NormalizedBar
     * 
     * @param rawBar 上游Bar数据
     * @return 标准化的Bar，barTime必须是bar_close_time语义
     */
    NormalizedBar normalize(Object rawBar);
    
    /**
     * 判断是否支持该类型的数据源
     * 
     * @param rawBar 上游Bar数据
     * @return 是否支持
     */
    boolean supports(Object rawBar);
}

