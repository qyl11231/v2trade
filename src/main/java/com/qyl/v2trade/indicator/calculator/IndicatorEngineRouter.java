package com.qyl.v2trade.indicator.calculator;

import com.qyl.v2trade.indicator.engine.IndicatorEngine;
import com.qyl.v2trade.indicator.repository.entity.IndicatorDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 指标引擎路由器（V2：扩展支持 impl_key）
 * 
 * <p>【V2 核心变化】
 * - 支持通过 `impl_key` 路由到对应引擎（优先）
 * - 保留通过 `engine` 字段路由的逻辑（向后兼容）
 * 
 * <p>【路由优先级】
 * 1. 优先使用 `impl_key`（如 "ta4j:rsi"）
 * 2. 回退到 `engine` 字段（如 "ta4j"）
 *
 * @author qyl
 */
@Slf4j
@Component
public class IndicatorEngineRouter {
    
    /**
     * 引擎存储：key = engineName（向后兼容）
     */
    private final Map<String, IndicatorEngine> engineMap = new ConcurrentHashMap<>();
    
    /**
     * impl_key 映射：key = implKey（V2新增）
     * 
     * <p>例如：ta4j:rsi -> Ta4jIndicatorEngine
     */
    private final Map<String, IndicatorEngine> implKeyMap = new ConcurrentHashMap<>();
    
    /**
     * 初始化：注册 impl_key 映射
     * 
     * <p>注意：引擎注册由 IndicatorEngineConfig 负责
     * <p>此方法只负责在引擎注册完成后，初始化 impl_key 映射
     */
  
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
     * 注册 impl_key 映射（V2新增）
     * 
     * <p>将 impl_key 映射到对应的引擎
     * <p>例如：ta4j:rsi -> Ta4jIndicatorEngine
     * 
     * <p>注意：此方法应在所有引擎注册完成后调用
     */
    public void registerImplKeys() {
        // 获取 ta4j 引擎
        IndicatorEngine ta4jEngine = engineMap.get("ta4j");
        if (ta4jEngine != null) {
            // 注册 ta4j 相关的 impl_key
            implKeyMap.put("ta4j:rsi", ta4jEngine);
            implKeyMap.put("ta4j:macd", ta4jEngine);
            implKeyMap.put("ta4j:sma", ta4jEngine);
            implKeyMap.put("ta4j:ema", ta4jEngine);
            implKeyMap.put("ta4j:boll", ta4jEngine);
            implKeyMap.put("ta4j:atr", ta4jEngine);
            implKeyMap.put("ta4j:stochastic", ta4jEngine);
            implKeyMap.put("ta4j:momentum", ta4jEngine);
            implKeyMap.put("ta4j:wma", ta4jEngine);
            implKeyMap.put("ta4j:kdj", ta4jEngine);
            log.debug("注册了 {} 个 ta4j impl_key 映射", implKeyMap.size());
        }
        
        // 可以继续注册其他引擎的 impl_key 映射
        // 例如：custom:xxx -> CustomEngine
    }
    
    /**
     * 获取引擎（V2：支持 impl_key 和 engine 字段）
     * 
     * <p>【路由优先级】
     * 1. 优先使用 `impl_key`（如果存在）
     * 2. 回退到 `engine` 字段（向后兼容）
     * 
     * @param definition 指标定义
     * @return 引擎实例，如果不存在返回null
     */
    public IndicatorEngine getEngine(IndicatorDefinition definition) {
        if (definition == null) {
            return null;
        }
        
        // 优先使用 impl_key
        String implKey = definition.getImplKey();
        if (implKey != null && !implKey.isEmpty()) {
            IndicatorEngine engine = implKeyMap.get(implKey);
            if (engine != null) {
                log.debug("通过 impl_key 路由到引擎: implKey={}, engineName={}", implKey, engine.getEngineName());
                return engine;
            } else {
                log.warn("impl_key 映射不存在: implKey={}, 回退到 engine 字段", implKey);
            }
        }
        
        // 回退到 engine 字段（向后兼容）
        String engineName = definition.getEngine();
        if (engineName != null && !engineName.isEmpty()) {
            IndicatorEngine engine = engineMap.get(engineName);
            if (engine != null) {
                log.debug("通过 engine 字段路由到引擎: engineName={}", engineName);
                return engine;
            }
        }
        
        log.warn("找不到引擎: implKey={}, engine={}", implKey, engineName);
        return null;
    }
    
    /**
     * 获取引擎（向后兼容方法）
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

