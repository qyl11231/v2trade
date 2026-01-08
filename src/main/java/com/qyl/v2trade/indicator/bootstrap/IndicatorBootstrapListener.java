package com.qyl.v2trade.indicator.bootstrap;

import com.qyl.v2trade.indicator.series.BarSeriesManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 指标模块启动监听器
 * 
 * <p>在系统启动时初始化指标模块
 * 
 * <p>顺序：
 * 1. QuestDbTsSemanticsProbe（阶段0，Order=100）
 * 2. IndicatorBootstrapListener（阶段1，Order=200）
 *
 * @author qyl
 */
@Slf4j
@Component
@Order(200) // 在QuestDbTsSemanticsProbe之后执行
public class IndicatorBootstrapListener implements CommandLineRunner {
    
    @Autowired
    private BarSeriesManager barSeriesManager;
    
    @Override
    public void run(String... args) {
        log.info("========== 开始初始化指标模块（从行情订阅配置加载K线数据） ==========");
        
        try {
            // 加载BarSeries历史数据（从行情订阅配置获取交易对）
            barSeriesManager.bootstrap();
            
            log.info("========== 指标模块初始化完成 ==========");
            
        } catch (Exception e) {
            log.error("========== 指标模块初始化失败 ==========", e);
            // 不抛出异常，允许系统继续启动（K线数据可以后续通过BarClosedEvent填充）
            log.warn("指标模块初始化失败，但系统将继续启动。K线数据可通过后续BarClosedEvent填充");
        }
    }
}

