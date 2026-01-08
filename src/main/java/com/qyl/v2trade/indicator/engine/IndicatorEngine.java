package com.qyl.v2trade.indicator.engine;

import com.qyl.v2trade.indicator.series.BarSeriesView;

/**
 * 指标计算引擎接口
 * 
 * <p>所有指标计算引擎必须实现此接口
 * 
 * <p>契约：
 * - 相同输入 -> 相同输出（确定性）
 * - 异常 -> 返回INVALID状态的IndicatorResult
 * - series的时间戳语义已是bar_close_time（UTC）
 * - 不暴露底层库类型到外层
 *
 * @author qyl
 */
public interface IndicatorEngine {
    
    /**
     * 计算指标
     * 
     * @param request 计算请求
     * @param series BarSeries只读视图
     * @return 计算结果
     */
    IndicatorResult compute(IndicatorComputeRequest request, BarSeriesView series);
    
    /**
     * 获取引擎名称
     * 
     * @return 引擎名称（如："ta4j", "custom"）
     */
    String getEngineName();
}

