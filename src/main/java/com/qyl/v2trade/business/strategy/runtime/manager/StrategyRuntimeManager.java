package com.qyl.v2trade.business.strategy.runtime.manager;

import com.qyl.v2trade.business.strategy.mapper.StrategyInstanceMapper;
import com.qyl.v2trade.business.strategy.model.entity.StrategyInstance;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qyl.v2trade.business.strategy.runtime.dispatch.StripedSerialExecutor;
import com.qyl.v2trade.business.strategy.runtime.runtime.StrategyRuntime;
import com.qyl.v2trade.business.strategy.runtime.state.StrategyState;
import com.qyl.v2trade.business.strategy.runtime.state.StrategyStateRepository;
import com.qyl.v2trade.business.strategy.runtime.trigger.StrategyTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 策略运行时管理器
 * 
 * <p>负责启动装载、管理 runtime、对外 dispatch
 *
 * @author qyl
 */
@Component
public class StrategyRuntimeManager {
    
    private static final Logger log = LoggerFactory.getLogger(StrategyRuntimeManager.class);
    
    @Autowired
    private StrategyInstanceMapper instanceMapper;
    
    @Autowired
    private StrategyStateRepository stateRepo;
    
    @Autowired
    private StripedSerialExecutor executor;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // Runtime 注册表（key = instanceId）
    private final ConcurrentHashMap<Long, StrategyRuntime> runtimes = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void initialize() {
        log.info("开始初始化 StrategyRuntimeManager...");
        
        // 1. 加载所有启用的实例
        List<StrategyInstance> enabledInstances = instanceMapper.selectAllEnabled();
        log.info("发现 {} 个启用的策略实例", enabledInstances.size());
        
        // 2. 为每个实例创建 runtime
        int successCount = 0;
        for (StrategyInstance instance : enabledInstances) {
            try {
                createRuntime(instance);
                successCount++;
            } catch (Exception e) {
                log.error("创建 runtime 失败: instanceId={}", instance.getId(), e);
            }
        }
        
        log.info("StrategyRuntimeManager 初始化完成: runtimeCount={}/{}", successCount, enabledInstances.size());
    }
    
    /**
     * 创建 Runtime（用于启动装载或动态添加）
     * 
     * @param instance 策略实例
     */
    private void createRuntime(StrategyInstance instance) {
        // 1. 加载或初始化状态
        StrategyState initialState = stateRepo.loadOrInit(instance);
        
        // 2. 创建 runtime
        StrategyRuntime runtime = new StrategyRuntime(instance, initialState, stateRepo, executor, objectMapper);
        
        // 3. 注册
        StrategyRuntime existing = runtimes.putIfAbsent(instance.getId(), runtime);
        if (existing != null) {
            log.warn("Runtime 已存在: instanceId={}", instance.getId());
        } else {
            log.info("Runtime 已创建: instanceId={}, phase={}", 
                instance.getId(), initialState.getPhase());
        }
    }
    
    /**
     * 分发事件到对应的 Runtime（由 TriggerDispatcher 调用）
     * 
     * @param instanceId 实例ID
     * @param trigger 触发事件
     */
    public void dispatch(Long instanceId, StrategyTrigger trigger) {
        StrategyRuntime runtime = runtimes.get(instanceId);
        if (runtime == null) {
            log.warn("Runtime 不存在，跳过事件: instanceId={}, eventKey={}", 
                instanceId, trigger.getEventKey());
            return;
        }
        
        runtime.onTrigger(trigger);
    }
    
    /**
     * 获取 Runtime（用于查询 API）
     * 
     * @param instanceId 实例ID
     * @return Runtime，不存在返回 null
     */
    public StrategyRuntime getRuntime(Long instanceId) {
        return runtimes.get(instanceId);
    }
    
    /**
     * 获取所有 Runtime（用于查询 API）
     * 
     * @return Runtime 列表
     */
    public List<StrategyRuntime> getAllRuntimes() {
        return new ArrayList<>(runtimes.values());
    }
    
    /**
     * 获取 Runtime 数量
     * 
     * @return Runtime 数量
     */
    public int getRuntimeCount() {
        return runtimes.size();
    }
    
    @PreDestroy
    public void shutdown() {
        log.info("StrategyRuntimeManager 正在关闭...");
        runtimes.clear();
        log.info("StrategyRuntimeManager 已关闭");
    }
}

