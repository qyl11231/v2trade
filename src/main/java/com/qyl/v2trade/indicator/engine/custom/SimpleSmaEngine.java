package com.qyl.v2trade.indicator.engine.custom;

import com.qyl.v2trade.indicator.domain.model.NormalizedBar;
import com.qyl.v2trade.indicator.engine.IndicatorComputeRequest;
import com.qyl.v2trade.indicator.engine.IndicatorEngine;
import com.qyl.v2trade.indicator.engine.IndicatorResult;
import com.qyl.v2trade.indicator.series.BarSeriesView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 自定义SMA引擎（示例）
 * 
 * <p>用于验证引擎可插拔性
 * 
 * <p>实现简单的SMA计算逻辑（不使用ta4j）
 *
 * @author qyl
 */
@Slf4j
@Component
public class SimpleSmaEngine implements IndicatorEngine {
    
    @Override
    public String getEngineName() {
        return "custom";
    }
    
    @Override
    public IndicatorResult compute(IndicatorComputeRequest request, BarSeriesView series) {
        try {
            // 1. 验证输入
            if (request == null || series == null) {
                return IndicatorResult.invalid("请求或系列数据为空");
            }
            
            if (!"SMA".equals(request.indicatorCode())) {
                return IndicatorResult.invalid("此引擎仅支持SMA指标");
            }
            
            // 2. 获取参数
            int period = getIntParam(request.parameters(), "period", 20);
            
            if (period <= 0) {
                return IndicatorResult.invalid("period必须大于0");
            }
            
            // 3. 获取目标时间之前的bars
            List<NormalizedBar> bars = series.getBars();
            
            // 找到目标时间的索引
            int targetIndex = findTargetIndex(bars, request.targetBarTime());
            if (targetIndex < 0) {
                return IndicatorResult.invalid("找不到目标时间的Bar: " + request.targetBarTime());
            }
            
            // 4. 验证数据是否充足
            if (targetIndex + 1 < period) {
                return IndicatorResult.invalid("数据不足，需要至少" + period + "根Bar，当前只有" + (targetIndex + 1) + "根");
            }
            
            // 5. 计算SMA
            BigDecimal sum = BigDecimal.ZERO;
            int startIndex = targetIndex - period + 1;
            
            for (int i = startIndex; i <= targetIndex; i++) {
                NormalizedBar bar = bars.get(i);
                sum = sum.add(bar.close());
            }
            
            BigDecimal sma = sum.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
            
            // 6. 返回结果
            return IndicatorResult.success("value", sma);
            
        } catch (Exception e) {
            log.error("SimpleSMA计算失败: error={}", e.getMessage(), e);
            return IndicatorResult.invalid("计算失败: " + e.getMessage());
        }
    }
    
    /**
     * 查找目标时间的索引
     */
    private int findTargetIndex(List<NormalizedBar> bars, LocalDateTime targetTime) {
        for (int i = 0; i < bars.size(); i++) {
            NormalizedBar bar = bars.get(i);
            if (bar.barTime().equals(targetTime)) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * 获取int参数
     */
    private int getIntParam(Map<String, Object> params, String key, int defaultValue) {
        if (params == null) {
            return defaultValue;
        }
        Object value = params.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}

