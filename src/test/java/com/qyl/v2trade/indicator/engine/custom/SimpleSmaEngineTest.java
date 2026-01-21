package com.qyl.v2trade.indicator.engine.custom;

import com.qyl.v2trade.indicator.domain.model.NormalizedBar;
import com.qyl.v2trade.indicator.engine.IndicatorComputeRequest;
import com.qyl.v2trade.indicator.engine.IndicatorResult;
import com.qyl.v2trade.indicator.series.BarSeriesView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * SimpleSmaEngine测试
 */
public class SimpleSmaEngineTest {
    
    private SimpleSmaEngine engine;
    private BarSeriesView mockSeries;
    
    @BeforeEach
    public void setUp() {
        engine = new SimpleSmaEngine();
        mockSeries = mock(BarSeriesView.class);
    }
    
    @Test
    public void testComputeSMA() {
        // 准备数据：创建20根Bar
        List<NormalizedBar> bars = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.of(2024, 1, 1, 0, 0);
        
        // 创建20根Bar，收盘价从100到119
        for (int i = 0; i < 20; i++) {
            bars.add(NormalizedBar.of(
                    1L, "BTC-USDT", "1h",
                    baseTime.plusHours(i),
                    BigDecimal.valueOf(100 + i),
                    BigDecimal.valueOf(101 + i),
                    BigDecimal.valueOf(99 + i),
                    BigDecimal.valueOf(100 + i),
                    BigDecimal.valueOf(1000)
            ));
        }
        
        when(mockSeries.getBars()).thenReturn(bars);
        
        // 计算最后一个Bar的SMA(20)
        IndicatorComputeRequest request = new IndicatorComputeRequest(
                "SMA",
                "v1",
                Map.of("period", 20),
                1L,
                "1h",
                baseTime.plusHours(19) // 最后一个Bar的时间
        );
        
        IndicatorResult result = engine.compute(request, mockSeries);
        
        // 验证结果
        assertThat(result.status()).isEqualTo(IndicatorResult.Status.SUCCESS);
        assertThat(result.values()).isNotNull();
        BigDecimal sma = result.getSingleValue();
        
        // SMA应该是 (100+101+...+119) / 20 = 109.5
        BigDecimal expected = BigDecimal.valueOf(109.5);
        assertThat(sma).isCloseTo(expected, org.assertj.core.data.Offset.offset(BigDecimal.valueOf(0.01)));
    }
    
    @Test
    public void testComputeSMAInsufficientData() {
        // 准备数据：只有10根Bar，但需要20根
        List<NormalizedBar> bars = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.of(2024, 1, 1, 0, 0);
        
        for (int i = 0; i < 10; i++) {
            bars.add(NormalizedBar.of(
                    1L, "BTC-USDT", "1h",
                    baseTime.plusHours(i),
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(101),
                    BigDecimal.valueOf(99),
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(1000)
            ));
        }
        
        when(mockSeries.getBars()).thenReturn(bars);
        
        IndicatorComputeRequest request = new IndicatorComputeRequest(
                "SMA",
                "v1",
                Map.of("period", 20),
                1L,
                "1h",
                baseTime.plusHours(9)
        );
        
        IndicatorResult result = engine.compute(request, mockSeries);
        
        // 验证失败
        assertThat(result.status()).isEqualTo(IndicatorResult.Status.INVALID);
        assertThat(result.errorMessage()).contains("数据不足");
    }
    
    @Test
    public void testDeterministicOutput() {
        // 验证相同输入 -> 相同输出（确定性）
        List<NormalizedBar> bars = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.of(2024, 1, 1, 0, 0);
        
        for (int i = 0; i < 20; i++) {
            bars.add(NormalizedBar.of(
                    1L, "BTC-USDT", "1h",
                    baseTime.plusHours(i),
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(101),
                    BigDecimal.valueOf(99),
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(1000)
            ));
        }
        
        when(mockSeries.getBars()).thenReturn(bars);
        
        IndicatorComputeRequest request = new IndicatorComputeRequest(
                "SMA",
                "v1",
                Map.of("period", 20),
                1L,
                "1h",
                baseTime.plusHours(19)
        );
        
        // 计算两次
        IndicatorResult result1 = engine.compute(request, mockSeries);
        IndicatorResult result2 = engine.compute(request, mockSeries);
        
        // 验证结果完全一致
        assertThat(result1.status()).isEqualTo(result2.status());
        assertThat(result1.getSingleValue()).isEqualByComparingTo(result2.getSingleValue());
    }
}

