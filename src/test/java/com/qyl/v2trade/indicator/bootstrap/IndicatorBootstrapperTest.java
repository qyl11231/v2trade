package com.qyl.v2trade.indicator.bootstrap;

import com.qyl.v2trade.indicator.definition.IndicatorDefinition;
import com.qyl.v2trade.indicator.definition.IndicatorRegistry;
import com.qyl.v2trade.indicator.repository.IndicatorDefinitionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IndicatorBootstrapper验收测试
 * 
 * <p>验证：
 * 1. 系统启动时自动注册内置指标定义（user_id=0）
 * 2. 重启不重复插入、不报错
 *
 * @author qyl
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext
public class IndicatorBootstrapperTest {
    
    @Autowired
    private IndicatorRegistry indicatorRegistry;
    
    @Autowired
    private IndicatorDefinitionRepository definitionRepository;
    
    @Test
    public void testSystemDefinitionsRegistered() {
        // 验证内存Registry中有指标定义
        List<IndicatorDefinition> allDefs = indicatorRegistry.getAllDefinitions();
        assertThat(allDefs).isNotEmpty();
        
        // 验证常见指标存在
        IndicatorDefinition rsi = indicatorRegistry.getDefinition("RSI", "v1");
        assertThat(rsi).isNotNull();
        assertThat(rsi.name()).isEqualTo("相对强弱指标");
        
        IndicatorDefinition macd = indicatorRegistry.getDefinition("MACD", "v1");
        assertThat(macd).isNotNull();
        
        IndicatorDefinition sma = indicatorRegistry.getDefinition("SMA", "v1");
        assertThat(sma).isNotNull();
    }
    
    @Test
    public void testSystemDefinitionsPersisted() {
        // 验证数据库中有系统指标定义（user_id=0）
        List<com.qyl.v2trade.indicator.repository.entity.IndicatorDefinition> systemDefs = 
                definitionRepository.listEnabled(0L);
        assertThat(systemDefs).isNotEmpty();
        
        // 验证至少包含常见指标
        boolean hasRSI = systemDefs.stream()
                .anyMatch(d -> "RSI".equals(d.getIndicatorCode()) && "v1".equals(d.getIndicatorVersion()));
        assertThat(hasRSI).isTrue();
        
        boolean hasMACD = systemDefs.stream()
                .anyMatch(d -> "MACD".equals(d.getIndicatorCode()) && "v1".equals(d.getIndicatorVersion()));
        assertThat(hasMACD).isTrue();
        
        // 验证所有都是user_id=0
        boolean allSystem = systemDefs.stream()
                .allMatch(d -> d.getUserId() != null && d.getUserId() == 0L);
        assertThat(allSystem).isTrue();
    }
    
    @Test
    public void testNoDuplicateOnRestart() {
        // 第一次启动后记录数量
        List<com.qyl.v2trade.indicator.repository.entity.IndicatorDefinition> firstRun = 
                definitionRepository.listEnabled(0L);
        int firstCount = firstRun.size();
        
        // 验证记录数量不变（如果之前已经执行过）
        // 注意：实际测试中，系统启动时会自动执行IndicatorBootstrapper，
        // 这里只验证去重逻辑是否正常工作
        List<com.qyl.v2trade.indicator.repository.entity.IndicatorDefinition> secondRun = 
                definitionRepository.listEnabled(0L);
        int secondCount = secondRun.size();
        
        // 数量应该相等（或者第二次因为去重而相等）
        assertThat(secondCount).isGreaterThanOrEqualTo(firstCount);
        
        // 验证没有重复记录（基于唯一键：user_id + code + version）
        long uniqueCount = secondRun.stream()
                .map(d -> d.getUserId() + ":" + d.getIndicatorCode() + ":" + d.getIndicatorVersion())
                .distinct()
                .count();
        assertThat(uniqueCount).isEqualTo(secondCount);
    }
}

