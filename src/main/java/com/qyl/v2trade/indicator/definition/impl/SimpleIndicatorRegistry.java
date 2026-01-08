package com.qyl.v2trade.indicator.definition.impl;

import com.qyl.v2trade.indicator.definition.IndicatorDefinition;
import com.qyl.v2trade.indicator.definition.IndicatorRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 简单的指标注册表实现（内存存储）
 *
 * @author qyl
 */
@Slf4j
@Component
public class SimpleIndicatorRegistry implements IndicatorRegistry {
    
    /**
     * 指标存储：key = code:version
     */
    private final ConcurrentMap<String, IndicatorDefinition> definitions = new ConcurrentHashMap<>();
    
    @Override
    public void register(IndicatorDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("指标定义不能为null");
        }
        
        String key = buildKey(definition.code(), definition.version());
        
        if (definitions.containsKey(key)) {
            log.warn("指标定义已存在，覆盖: code={}, version={}", 
                    definition.code(), definition.version());
        }
        
        definitions.put(key, definition);
        log.debug("注册指标定义: code={}, version={}, name={}",
                definition.code(), definition.version(), definition.name());
    }
    
    @Override
    public IndicatorDefinition getDefinition(String code, String version) {
        String key = buildKey(code, version);
        return definitions.get(key);
    }
    
    @Override
    public List<IndicatorDefinition> getAllDefinitions() {
        return new ArrayList<>(definitions.values());
    }
    
    @Override
    public boolean isRegistered(String code, String version) {
        String key = buildKey(code, version);
        return definitions.containsKey(key);
    }
    
    private String buildKey(String code, String version) {
        return code + ":" + version;
    }
}

