package com.qyl.v2trade.business.strategy.runtime.router;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.qyl.v2trade.business.strategy.mapper.StrategyInstanceMapper;
import com.qyl.v2trade.business.strategy.model.entity.StrategyInstance;
import com.qyl.v2trade.business.strategy.runtime.trigger.StrategyTrigger;
import com.qyl.v2trade.business.strategy.runtime.trigger.TriggerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 带缓存的事件路由器
 * 
 * <p>使用 Caffeine 缓存实现路由索引，避免每个事件都查 DB
 *
 * @author qyl
 */
@Component
public class CachedEventRouter implements EventRouter {
    
    private static final Logger logger = LoggerFactory.getLogger(CachedEventRouter.class);
    
    @Autowired
    private StrategyInstanceMapper instanceMapper;
    
    // 索引缓存：tradingPairId -> List<instanceId>
    private Cache<Long, List<Long>> pairIndexCache;
    
    // 索引缓存：signalConfigId -> List<instanceId>
    private Cache<Long, List<Long>> signalIndexCache;
    
    // 实例ID -> 用户ID 缓存（用于 TriggerLogger，避免查库）
    private Cache<Long, Long> instanceUserIdCache;
    
    public CachedEventRouter() {
        // TTL 60s 自动刷新（不依赖 UI/事件联动）
        this.pairIndexCache = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .maximumSize(10_000)
            .build();
        
        this.signalIndexCache = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .maximumSize(10_000)
            .build();
        
        this.instanceUserIdCache = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .maximumSize(10_000)
            .build();
    }
    
    @PostConstruct
    public void warmup() {
        // 启动时预热索引
        refreshAllIndexes();
        logger.info("EventRouter 索引预热完成");
    }
    
    @Override
    public List<Long> route(StrategyTrigger trigger) {
        TriggerType type = trigger.getTriggerType();
        
        switch (type) {
            case BAR_CLOSE:
            case PRICE:
                return routeByTradingPair(trigger.getTradingPairId());
                
            case SIGNAL:
                return routeBySignalConfig(trigger.getSignalConfigId());
                
            default:
                logger.warn("未知的触发类型: {}", type);
                return List.of();
        }
    }
    
    private List<Long> routeByTradingPair(Long tradingPairId) {
        if (tradingPairId == null) {
            return List.of();
        }
        
        return pairIndexCache.get(tradingPairId, key -> {
            List<StrategyInstance> instances = instanceMapper.selectEnabledByTradingPairId(key);
            List<Long> instanceIds = instances.stream()
                .map(StrategyInstance::getId)
                .collect(Collectors.toList());
            
            // 同时更新 userId 缓存
            instances.forEach(inst -> {
                instanceUserIdCache.put(inst.getId(), inst.getUserId());
            });
            
            return instanceIds;
        });
    }
    
    private List<Long> routeBySignalConfig(Long signalConfigId) {
        if (signalConfigId == null) {
            return List.of();
        }
        
        return signalIndexCache.get(signalConfigId, key -> {
            List<StrategyInstance> instances = instanceMapper.selectEnabledBySignalConfigId(key);
            List<Long> instanceIds = instances.stream()
                .map(StrategyInstance::getId)
                .collect(Collectors.toList());
            
            // 同时更新 userId 缓存
            instances.forEach(inst -> {
                instanceUserIdCache.put(inst.getId(), inst.getUserId());
            });
            
            return instanceIds;
        });
    }
    
    @Override
    public Long getUserIdByInstanceId(Long instanceId) {
        if (instanceId == null) {
            return null;
        }
        return instanceUserIdCache.getIfPresent(instanceId);
    }
    
    /**
     * 刷新所有索引（可用于手动触发或定时刷新）
     */
    public void refreshAllIndexes() {
        List<StrategyInstance> allEnabled = instanceMapper.selectAllEnabled();
        
        // 按 tradingPairId 分组
        Map<Long, List<Long>> pairMap = allEnabled.stream()
            .collect(Collectors.groupingBy(
                StrategyInstance::getTradingPairId,
                Collectors.mapping(StrategyInstance::getId, Collectors.toList())
            ));
        pairMap.forEach((pairId, instanceIds) -> {
            pairIndexCache.put(pairId, instanceIds);
        });
        
        // 按 signalConfigId 分组（注意：signalConfigId 可能为 null，需要处理）
        Map<Long, List<Long>> signalMap = allEnabled.stream()
            .filter(inst -> inst.getSignalConfigId() != null && inst.getSignalConfigId() > 0)
            .collect(Collectors.groupingBy(
                StrategyInstance::getSignalConfigId,
                Collectors.mapping(StrategyInstance::getId, Collectors.toList())
            ));
        signalMap.forEach((signalId, instanceIds) -> {
            signalIndexCache.put(signalId, instanceIds);
        });
        
        // 同时构建 instanceId -> userId 缓存（用于 TriggerLogger，避免查库）
        allEnabled.forEach(inst -> {
            instanceUserIdCache.put(inst.getId(), inst.getUserId());
        });
        
        logger.debug("索引刷新完成: pairIndex={}, signalIndex={}, instanceUserCache={}", 
            pairMap.size(), signalMap.size(), instanceUserIdCache.estimatedSize());
    }
}

