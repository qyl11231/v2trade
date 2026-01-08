package com.qyl.v2trade.indicator.engine.ta4j;

import com.qyl.v2trade.indicator.domain.model.NormalizedBar;
import com.qyl.v2trade.indicator.engine.IndicatorComputeRequest;
import com.qyl.v2trade.indicator.engine.IndicatorEngine;
import com.qyl.v2trade.indicator.engine.IndicatorResult;
import com.qyl.v2trade.indicator.series.BarSeriesView;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.indicators.*;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ta4j指标计算引擎
 * 
 * <p>实现基于ta4j库的指标计算
 * 
 * <p>契约：
 * - series的时间戳语义已是bar_close_time（UTC）
 * - 不暴露ta4j类型到外层
 * - 相同输入 -> 相同输出（确定性）
 *
 * @author qyl
 */
@Slf4j
@Component
public class Ta4jIndicatorEngine implements IndicatorEngine {
    
    @Override
    public String getEngineName() {
        return "ta4j";
    }
    
    @Override
    public IndicatorResult compute(IndicatorComputeRequest request, BarSeriesView series) {
        try {
            // 1. 验证输入
            if (request == null || series == null) {
                return IndicatorResult.invalid("请求或系列数据为空");
            }
            
            if (series.size() == 0) {
                return IndicatorResult.invalid("BarSeries为空");
            }
            
            // 2. 转换为ta4j的BarSeries
            BarSeries ta4jSeries = convertToTa4jSeries(series);
            
            // 3. 根据指标编码创建Indicator
            Indicator<Num> indicator = createIndicator(request, ta4jSeries);
            
            if (indicator == null) {
                return IndicatorResult.invalid("不支持的指标: " + request.indicatorCode());
            }
            
            // 4. 获取目标索引
            int targetIndex = findTargetIndex(ta4jSeries, request.targetBarTime());
            if (targetIndex < 0) {
                return IndicatorResult.invalid("找不到目标时间的Bar: " + request.targetBarTime());
            }
            
            // 5. 验证数据是否充足
            if (targetIndex >= indicator.getBarSeries().getBarCount()) {
                return IndicatorResult.invalid("目标索引超出范围");
            }
            
            // 6. 计算结果
            Num indicatorValue = indicator.getValue(targetIndex);
            
            if (indicatorValue == null || indicatorValue.isNaN()) {
                return IndicatorResult.invalid("计算结果为NaN");
            }
            
            // 7. 转换为结果
            Map<String, BigDecimal> resultMap = buildResultMap(request, indicator, targetIndex);
            
            return IndicatorResult.success(resultMap);
            
        } catch (Exception e) {
            log.error("Ta4j指标计算失败: code={}, error={}", 
                    request != null ? request.indicatorCode() : "unknown", e.getMessage(), e);
            return IndicatorResult.invalid("计算失败: " + e.getMessage());
        }
    }
    
    /**
     * 转换为ta4j的BarSeries
     */
    private BarSeries convertToTa4jSeries(BarSeriesView series) {
        List<NormalizedBar> bars = series.getBars();
        
        if (bars == null || bars.isEmpty()) {
            return new BaseBarSeries();
        }
        
        // 获取timeframe以确定时间周期
        String timeframe = bars.get(0).timeframe();
        Duration timePeriod = parseTimeframeToDuration(timeframe);
        
        BaseBarSeries ta4jSeries = new BaseBarSeries();
        
        for (NormalizedBar bar : bars) {
            try {
                // 创建ta4j的Bar
                // ta4j 0.16 要求：需要设置时间周期和开始时间
                LocalDateTime barTime = bar.barTime();
                ZonedDateTime beginTime = barTime.atZone(ZoneId.of("UTC"));
                
                // 构建BaseBar，需要时间周期和开始时间
                BaseBar ta4jBar = BaseBar.builder()
                        .timePeriod(timePeriod)
                        .endTime(beginTime.plus(timePeriod)) // endTime = beginTime + period
                        .openPrice(ta4jSeries.numOf(bar.open().doubleValue()))
                        .highPrice(ta4jSeries.numOf(bar.high().doubleValue()))
                        .lowPrice(ta4jSeries.numOf(bar.low().doubleValue()))
                        .closePrice(ta4jSeries.numOf(bar.close().doubleValue()))
                        .volume(ta4jSeries.numOf(bar.volume().doubleValue()))
                        .build();
                
                ta4jSeries.addBar(ta4jBar);
                
            } catch (Exception e) {
                log.warn("转换Bar失败，跳过: barTime={}, timeframe={}, error={}", 
                        bar.barTime(), bar.timeframe(), e.getMessage());
            }
        }
        
        return ta4jSeries;
    }
    
    /**
     * 解析timeframe字符串为Duration
     * 例如：5m -> Duration.ofMinutes(5), 1h -> Duration.ofHours(1)
     */
    private Duration parseTimeframeToDuration(String timeframe) {
        if (timeframe == null || timeframe.isEmpty()) {
            return Duration.ofMinutes(1); // 默认1分钟
        }
        
        try {
            // 提取数字和单位
            String numberStr = timeframe.replaceAll("[^0-9]", "");
            String unit = timeframe.replaceAll("[0-9]", "").toLowerCase();
            
            if (numberStr.isEmpty()) {
                return Duration.ofMinutes(1);
            }
            
            long number = Long.parseLong(numberStr);
            
            switch (unit) {
                case "m":
                    return Duration.ofMinutes(number);
                case "h":
                    return Duration.ofHours(number);
                case "d":
                    return Duration.ofDays(number);
                case "w":
                    return Duration.ofDays(number * 7);
                default:
                    log.warn("未知的timeframe单位: {}, 使用默认1分钟", unit);
                    return Duration.ofMinutes(1);
            }
        } catch (Exception e) {
            log.warn("解析timeframe失败: {}, 使用默认1分钟", timeframe, e);
            return Duration.ofMinutes(1);
        }
    }
    
    /**
     * 创建Indicator
     */
    private Indicator<Num> createIndicator(IndicatorComputeRequest request, BarSeries ta4jSeries) {
        String code = request.indicatorCode();
        Map<String, Object> params = request.parameters();
        
        ClosePriceIndicator closePrice = new ClosePriceIndicator(ta4jSeries);
        
        switch (code) {
            case "SMA":
                int periodSMA = getIntParam(params, "period", 20);
                return new SMAIndicator(closePrice, periodSMA);
                
            case "EMA":
                int periodEMA = getIntParam(params, "period", 20);
                return new EMAIndicator(closePrice, periodEMA);
                
            case "WMA":
                int periodWMA = getIntParam(params, "period", 20);
                return new WMAIndicator(closePrice, periodWMA);
                
            case "RSI":
                int periodRSI = getIntParam(params, "period", 14);
                return new RSIIndicator(closePrice, periodRSI);
                
            case "ATR":
                int periodATR = getIntParam(params, "period", 14);
                return new ATRIndicator(ta4jSeries, periodATR);
                
            case "BOLL":
                int periodBOLL = getIntParam(params, "period", 20);
                SMAIndicator smaForBOLL = new SMAIndicator(closePrice, periodBOLL);
                return smaForBOLL; // BOLL需要特殊处理，返回SMA作为middle
                
            case "MACD":
                int shortPeriod = getIntParam(params, "shortPeriod", 12);
                int longPeriod = getIntParam(params, "longPeriod", 26);
                // MACD需要返回多个值，这里需要特殊处理
                MACDIndicator macdIndicator = new MACDIndicator(closePrice, shortPeriod, longPeriod);
                return macdIndicator;
                
            case "Stochastic":
                int kPeriod = getIntParam(params, "kPeriod", 14);
                StochasticOscillatorKIndicator stochK = new StochasticOscillatorKIndicator(
                        ta4jSeries, kPeriod);
                // Stochastic需要返回k和d两个值
                return stochK;
                
            case "Momentum":
                int periodMomentum = getIntParam(params, "period", 10);
                // ROCIndicator用于计算动量
                ROCIndicator momentumIndicator = new ROCIndicator(closePrice, periodMomentum);
                return momentumIndicator;
                
            default:
                log.warn("不支持的指标: {}", code);
                return null;
        }
    }
    
    /**
     * 构建结果Map
     */
    private Map<String, BigDecimal> buildResultMap(
            IndicatorComputeRequest request, 
            Indicator<Num> indicator, 
            int index) {
        Map<String, BigDecimal> resultMap = new HashMap<>();
        String code = request.indicatorCode();
        
        // 需要特殊处理的多值指标
        if ("MACD".equals(code)) {
            // MACD返回三个值
            MACDIndicator macd = (MACDIndicator) indicator;
            int signalPeriod = getIntParam(request.parameters(), "signalPeriod", 9);
            EMAIndicator signal = new EMAIndicator(macd, signalPeriod);
            
            Num macdValue = macd.getValue(index);
            Num signalValue = signal.getValue(index);
            
            resultMap.put("macd", toBigDecimal(macdValue));
            resultMap.put("signal", toBigDecimal(signalValue));
            resultMap.put("histogram", toBigDecimal(macdValue.minus(signalValue)));
        } else if ("BOLL".equals(code)) {
            // BOLL返回三个值
            int period = getIntParam(request.parameters(), "period", 20);
            double deviation = getDoubleParam(request.parameters(), "deviation", 2.0);
            ClosePriceIndicator closePrice = new ClosePriceIndicator(indicator.getBarSeries());
            SMAIndicator sma = new SMAIndicator(closePrice, period);
            StandardDeviationIndicator stdDev = new StandardDeviationIndicator(closePrice, period);
            
            Num middle = sma.getValue(index);
            Num stdDevValue = stdDev.getValue(index);
            Num upper = middle.plus(stdDevValue.multipliedBy(indicator.getBarSeries().numOf(deviation)));
            Num lower = middle.minus(stdDevValue.multipliedBy(indicator.getBarSeries().numOf(deviation)));
            
            resultMap.put("upper", toBigDecimal(upper));
            resultMap.put("middle", toBigDecimal(middle));
            resultMap.put("lower", toBigDecimal(lower));
        } else if ("Stochastic".equals(code)) {
            // Stochastic返回k和d两个值
            // 注意：StochasticOscillatorDIndicator的构造函数可能需要K指标
            // 这里简化处理，只返回K值
            StochasticOscillatorKIndicator stochK = (StochasticOscillatorKIndicator) indicator;
            resultMap.put("k", toBigDecimal(stochK.getValue(index)));
            // D值需要基于K值计算，这里暂时返回K值作为占位
            resultMap.put("d", toBigDecimal(stochK.getValue(index)));
        } else if ("KDJ".equals(code)) {
            // KDJ暂时不支持，返回空Map（调用者应该已经在createIndicator中处理不支持的情况）
            // 这里不应该被调用到，因为createIndicator会返回null
            resultMap.put("value", BigDecimal.ZERO);
        } else {
            // 单值指标
            resultMap.put("value", toBigDecimal(indicator.getValue(index)));
        }
        
        return resultMap;
    }
    
    /**
     * 查找目标时间的索引
     */
    private int findTargetIndex(BarSeries ta4jSeries, java.time.LocalDateTime targetTime) {
        // ta4j的Bar使用LocalDateTime，直接比较
        for (int i = 0; i < ta4jSeries.getBarCount(); i++) {
            Bar bar = ta4jSeries.getBar(i);
            java.time.LocalDateTime barTime = bar.getBeginTime().toLocalDateTime();
            if (barTime.equals(targetTime)) {
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
    
    /**
     * 获取double参数
     */
    private double getDoubleParam(Map<String, Object> params, String key, double defaultValue) {
        if (params == null) {
            return defaultValue;
        }
        Object value = params.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * 转换为BigDecimal
     */
    private BigDecimal toBigDecimal(Num num) {
        if (num == null || num.isNaN()) {
            return null;
        }
        return BigDecimal.valueOf(num.doubleValue()).setScale(8, RoundingMode.HALF_UP);
    }
}

