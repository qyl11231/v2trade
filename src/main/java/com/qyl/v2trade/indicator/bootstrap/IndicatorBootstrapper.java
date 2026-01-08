package com.qyl.v2trade.indicator.bootstrap;

import com.qyl.v2trade.indicator.definition.IndicatorDefinition;
import com.qyl.v2trade.indicator.definition.IndicatorRegistry;
import com.qyl.v2trade.indicator.definition.builtin.BuiltinIndicatorDefinitions;
import com.qyl.v2trade.indicator.repository.IndicatorDefinitionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 指标模块启动器
 * 
 * <p>负责：
 * 1. 注册内置指标定义到内存Registry
 * 2. 将指标定义写入数据库（user_id=0）
 * 
 * <p>执行顺序：
 * - Order(150)：在QuestDbTsSemanticsProbe（100）之后，IndicatorBootstrapListener（200）之前
 *
 * @author qyl
 */
@Slf4j
@Component
@Order(150)
public class IndicatorBootstrapper implements CommandLineRunner {
    
    @Autowired
    private IndicatorRegistry indicatorRegistry;
    
    @Autowired
    private IndicatorDefinitionRepository definitionRepository;
    
    @Override
    public void run(String... args) {
        log.info("开始注册内置指标定义");
        
        try {
            // 1. 获取所有内置指标定义
            List<IndicatorDefinition> builtinDefs = 
                    BuiltinIndicatorDefinitions.getAllBuiltinDefinitions();
            
            log.info("发现{}个内置指标定义", builtinDefs.size());
            
            // 2. 注册到内存Registry
            for (IndicatorDefinition def : builtinDefs) {
                indicatorRegistry.register(def);
            }
            
            // 3. 转换为Entity并写入数据库
            List<com.qyl.v2trade.indicator.repository.entity.IndicatorDefinition> entities = new ArrayList<>();
            for (IndicatorDefinition def : builtinDefs) {
                com.qyl.v2trade.indicator.repository.entity.IndicatorDefinition entity = convertToEntity(def);
                entities.add(entity);
            }
            
            // 4. 批量保存（user_id=0，重复忽略）
            definitionRepository.saveSystemDefinitions(entities);
            
            log.info("内置指标定义注册完成: count={}", builtinDefs.size());
            
        } catch (Exception e) {
            log.error("注册内置指标定义失败", e);
            throw new RuntimeException("指标定义注册失败，系统启动中止", e);
        }
    }
    
    /**
     * 转换为Entity
     */
    private com.qyl.v2trade.indicator.repository.entity.IndicatorDefinition convertToEntity(
            IndicatorDefinition def) {
        com.qyl.v2trade.indicator.repository.entity.IndicatorDefinition entity = 
                new com.qyl.v2trade.indicator.repository.entity.IndicatorDefinition();
        entity.setUserId(0L); // 系统内置
        entity.setIndicatorCode(def.code());
        entity.setIndicatorName(def.name());
        entity.setIndicatorVersion(def.version());
        entity.setCategory(def.category().name());
        entity.setEngine(def.engine());
        entity.setParamSchema(def.parameters().toMap());
        entity.setReturnSchema(def.returns().toMap());
        entity.setMinRequiredBars(def.minRequiredBars());
        entity.setSupportedTimeframes(new ArrayList<>(def.supportedTimeframes()));
        entity.setEnabled(1);
        
        return entity;
    }
}

