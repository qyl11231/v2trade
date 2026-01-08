package com.qyl.v2trade.indicator.series;

import com.qyl.v2trade.indicator.domain.model.NormalizedBar;

import java.time.LocalDateTime;
import java.util.List;

/**
 * BarSeries只读视图
 * 
 * <p>用于指标计算引擎，提供只读访问
 *
 * @author qyl
 */
public interface BarSeriesView {
    
    /**
     * 获取所有bars（按时间升序）
     * 
     * @return bars列表
     */
    List<NormalizedBar> getBars();
    
    /**
     * 获取指定索引的bar
     * 
     * @param index 索引（0-based，0是最旧的bar）
     * @return bar
     */
    NormalizedBar getBar(int index);
    
    /**
     * 获取bars数量
     * 
     * @return 数量
     */
    int size();
    
    /**
     * 获取最新的bar
     * 
     * @return 最新的bar，如果没有返回null
     */
    NormalizedBar getLatestBar();
    
    /**
     * 获取指定时间之前的bars（用于计算指标）
     * 
     * @param beforeTime 时间（不包含）
     * @return bars列表
     */
    List<NormalizedBar> getBarsBefore(LocalDateTime beforeTime);
    
    /**
     * 获取交易对ID
     */
    Long getTradingPairId();
    
    /**
     * 获取周期
     */
    String getTimeframe();
}

