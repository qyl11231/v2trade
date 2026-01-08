package com.qyl.v2trade.indicator.definition.builtin;

import com.qyl.v2trade.indicator.definition.*;
import com.qyl.v2trade.indicator.definition.impl.SimpleIndicatorDefinition;

import java.util.List;
import java.util.Set;

/**
 * 内置指标定义
 * 
 * <p>v1.2.1阶段定义的系统内置指标
 *
 * @author qyl
 */
public class BuiltinIndicatorDefinitions {
    
    /**
     * 获取所有内置指标定义
     */
    public static List<IndicatorDefinition> getAllBuiltinDefinitions() {
        return List.of(
            // 趋势类
            createSMA(),
            createEMA(),
            createWMA(),
            
            // 动量类
            createRSI(),
            createStochastic(),
            createMomentum(),
            
            // 波动率
            createATR(),
            createBollingerBands(),
            
            // 复合指标
            createMACD(),
            createKDJ()
        );
    }
    
    // ========== 趋势类 ==========
    
    private static IndicatorDefinition createSMA() {
        return new SimpleIndicatorDefinition(
            "SMA",
            "v1",
            "简单移动平均",
            IndicatorCategory.TREND,
            "ta4j",
            ParameterSpec.of(List.of(
                new ParameterSpec.ParameterDefinition(
                    "period",
                    ParameterSpec.ParamType.INT,
                    true,
                    20,
                    new ParameterSpec.Range(1, 500)
                )
            )),
            ReturnSpec.single(),
            20, // minRequiredBars
            Set.of("1m", "5m", "15m", "30m", "1h", "4h")
        );
    }
    
    private static IndicatorDefinition createEMA() {
        return new SimpleIndicatorDefinition(
            "EMA",
            "v1",
            "指数移动平均",
            IndicatorCategory.TREND,
            "ta4j",
            ParameterSpec.of(List.of(
                new ParameterSpec.ParameterDefinition(
                    "period",
                    ParameterSpec.ParamType.INT,
                    true,
                    20,
                    new ParameterSpec.Range(1, 500)
                )
            )),
            ReturnSpec.single(),
            20,
            Set.of("1m", "5m", "15m", "30m", "1h", "4h")
        );
    }
    
    private static IndicatorDefinition createWMA() {
        return new SimpleIndicatorDefinition(
            "WMA",
            "v1",
            "加权移动平均",
            IndicatorCategory.TREND,
            "ta4j",
            ParameterSpec.of(List.of(
                new ParameterSpec.ParameterDefinition(
                    "period",
                    ParameterSpec.ParamType.INT,
                    true,
                    20,
                    new ParameterSpec.Range(1, 500)
                )
            )),
            ReturnSpec.single(),
            20,
            Set.of("1m", "5m", "15m", "30m", "1h", "4h")
        );
    }
    
    // ========== 动量类 ==========
    
    private static IndicatorDefinition createRSI() {
        return new SimpleIndicatorDefinition(
            "RSI",
            "v1",
            "相对强弱指标",
            IndicatorCategory.MOMENTUM,
            "ta4j",
            ParameterSpec.of(List.of(
                new ParameterSpec.ParameterDefinition(
                    "period",
                    ParameterSpec.ParamType.INT,
                    true,
                    14,
                    new ParameterSpec.Range(2, 100)
                )
            )),
            ReturnSpec.single(),
            14,
            Set.of("1m", "5m", "15m", "30m", "1h", "4h")
        );
    }
    
    private static IndicatorDefinition createStochastic() {
        return new SimpleIndicatorDefinition(
            "Stochastic",
            "v1",
            "随机指标",
            IndicatorCategory.MOMENTUM,
            "ta4j",
            ParameterSpec.of(List.of(
                new ParameterSpec.ParameterDefinition(
                    "kPeriod",
                    ParameterSpec.ParamType.INT,
                    true,
                    14,
                    new ParameterSpec.Range(1, 100)
                ),
                new ParameterSpec.ParameterDefinition(
                    "dPeriod",
                    ParameterSpec.ParamType.INT,
                    true,
                    3,
                    new ParameterSpec.Range(1, 100)
                )
            )),
            ReturnSpec.multi(List.of("k", "d")),
            14,
            Set.of("1m", "5m", "15m", "30m", "1h", "4h")
        );
    }
    
    private static IndicatorDefinition createMomentum() {
        return new SimpleIndicatorDefinition(
            "Momentum",
            "v1",
            "动量指标",
            IndicatorCategory.MOMENTUM,
            "ta4j",
            ParameterSpec.of(List.of(
                new ParameterSpec.ParameterDefinition(
                    "period",
                    ParameterSpec.ParamType.INT,
                    true,
                    10,
                    new ParameterSpec.Range(1, 100)
                )
            )),
            ReturnSpec.single(),
            10,
            Set.of("1m", "5m", "15m", "30m", "1h", "4h")
        );
    }
    
    // ========== 波动率 ==========
    
    private static IndicatorDefinition createATR() {
        return new SimpleIndicatorDefinition(
            "ATR",
            "v1",
            "平均真实波幅",
            IndicatorCategory.VOLATILITY,
            "ta4j",
            ParameterSpec.of(List.of(
                new ParameterSpec.ParameterDefinition(
                    "period",
                    ParameterSpec.ParamType.INT,
                    true,
                    14,
                    new ParameterSpec.Range(1, 100)
                )
            )),
            ReturnSpec.single(),
            14,
            Set.of("1m", "5m", "15m", "30m", "1h", "4h")
        );
    }
    
    private static IndicatorDefinition createBollingerBands() {
        return new SimpleIndicatorDefinition(
            "BOLL",
            "v1",
            "布林带",
            IndicatorCategory.VOLATILITY,
            "ta4j",
            ParameterSpec.of(List.of(
                new ParameterSpec.ParameterDefinition(
                    "period",
                    ParameterSpec.ParamType.INT,
                    true,
                    20,
                    new ParameterSpec.Range(1, 100)
                ),
                new ParameterSpec.ParameterDefinition(
                    "deviation",
                    ParameterSpec.ParamType.DECIMAL,
                    true,
                    2.0,
                    new ParameterSpec.Range(0.1, 5.0)
                )
            )),
            ReturnSpec.multi(List.of("upper", "middle", "lower")),
            20,
            Set.of("1m", "5m", "15m", "30m", "1h", "4h")
        );
    }
    
    // ========== 复合指标 ==========
    
    private static IndicatorDefinition createMACD() {
        return new SimpleIndicatorDefinition(
            "MACD",
            "v1",
            "MACD指标",
            IndicatorCategory.MOMENTUM,
            "ta4j",
            ParameterSpec.of(List.of(
                new ParameterSpec.ParameterDefinition(
                    "shortPeriod",
                    ParameterSpec.ParamType.INT,
                    true,
                    12,
                    new ParameterSpec.Range(1, 50)
                ),
                new ParameterSpec.ParameterDefinition(
                    "longPeriod",
                    ParameterSpec.ParamType.INT,
                    true,
                    26,
                    new ParameterSpec.Range(1, 100)
                ),
                new ParameterSpec.ParameterDefinition(
                    "signalPeriod",
                    ParameterSpec.ParamType.INT,
                    true,
                    9,
                    new ParameterSpec.Range(1, 50)
                )
            )),
            ReturnSpec.multi(List.of("macd", "signal", "histogram")),
            26,
            Set.of("1m", "5m", "15m", "30m", "1h", "4h")
        );
    }
    
    private static IndicatorDefinition createKDJ() {
        return new SimpleIndicatorDefinition(
            "KDJ",
            "v1",
            "KDJ指标",
            IndicatorCategory.MOMENTUM,
            "ta4j",
            ParameterSpec.of(List.of(
                new ParameterSpec.ParameterDefinition(
                    "kPeriod",
                    ParameterSpec.ParamType.INT,
                    true,
                    9,
                    new ParameterSpec.Range(1, 50)
                ),
                new ParameterSpec.ParameterDefinition(
                    "dPeriod",
                    ParameterSpec.ParamType.INT,
                    true,
                    3,
                    new ParameterSpec.Range(1, 50)
                ),
                new ParameterSpec.ParameterDefinition(
                    "jPeriod",
                    ParameterSpec.ParamType.INT,
                    true,
                    3,
                    new ParameterSpec.Range(1, 50)
                )
            )),
            ReturnSpec.multi(List.of("k", "d", "j")),
            9,
            Set.of("1m", "5m", "15m", "30m", "1h", "4h")
        );
    }
}

