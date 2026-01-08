package com.qyl.v2trade.indicator.config;

import com.qyl.v2trade.indicator.calculator.IndicatorEngineRouter;
import com.qyl.v2trade.indicator.engine.IndicatorEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * 指标引擎配置
 * 
 * <p>自动注册所有IndicatorEngine实现
 *
 * @author qyl
 */
@Slf4j
@Configuration
public class IndicatorEngineConfig {
    
    @Autowired
    private List<IndicatorEngine> engines;
    
    @Autowired
    private IndicatorEngineRouter engineRouter;
    
    @PostConstruct
    public void registerEngines() {
        log.info("开始注册指标引擎，共{}个", engines.size());
        engineRouter.registerEngines(engines);
        log.info("指标引擎注册完成");
    }
}

