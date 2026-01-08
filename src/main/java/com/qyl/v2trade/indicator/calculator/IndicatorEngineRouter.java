package com.qyl.v2trade.indicator.calculator;

import com.qyl.v2trade.indicator.engine.IndicatorEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 指标引擎路由器
 * 
 * <p>根据引擎名称路由到对应的引擎实现
 *
 * @author qyl
 */
@Slf4j
@Component
public class IndicatorEngineRouter {
    
    /**
     * 引擎存储：key = engineName
     */
    private final Map<String, IndicatorEngine> engineMap = new ConcurrentHashMap<>();
    
    /**
     * 注册引擎
     */
    public void registerEngine(IndicatorEngine engine) {
        if (engine == null) {
            throw new IllegalArgumentException("引擎不能为null");
        }
        
        String engineName = engine.getEngineName();
        if (engineMap.containsKey(engineName)) {
            log.warn("引擎已存在，覆盖: engineName={}", engineName);
        }
        
        engineMap.put(engineName, engine);
        log.debug("注册指标引擎: engineName={}", engineName);
    }
    
    /**
     * 批量注册引擎
     */
    public void registerEngines(List<IndicatorEngine> engines) {
        if (engines != null) {
            for (IndicatorEngine engine : engines) {
                registerEngine(engine);
            }
        }
    }
    
    /**
     * 获取引擎
     * 
     * @param engineName 引擎名称
     * @return 引擎实例，如果不存在返回null
     */
    public IndicatorEngine getEngine(String engineName) {
        return engineMap.get(engineName);
    }
    
    /**
     * 检查引擎是否存在
     */
    public boolean hasEngine(String engineName) {
        return engineMap.containsKey(engineName);
    }
}

