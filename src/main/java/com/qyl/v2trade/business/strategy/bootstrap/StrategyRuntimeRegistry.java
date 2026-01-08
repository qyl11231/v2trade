package com.qyl.v2trade.business.strategy.bootstrap;

import com.qyl.v2trade.business.strategy.factory.model.StrategyRuntime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 策略运行时注册表
 * 
 * <p>职责：
 * <ul>
 *   <li>管理所有策略运行时实例</li>
 *   <li>提供注册、注销、查询方法</li>
 *   <li>线程安全</li>
 * </ul>
 * 
 * <p>线程安全：
 * <ul>
 *   <li>使用 ConcurrentHashMap 保证线程安全</li>
 *   <li>所有操作都是原子操作</li>
 * </ul>
 */
@Slf4j
@Component
public class StrategyRuntimeRegistry {

    /**
     * 策略运行时映射（线程安全）
     * Key: strategyId
     * Value: StrategyRuntime
     */
    private final ConcurrentMap<Long, StrategyRuntime> runtimes = new ConcurrentHashMap<>();

    /**
     * 注册策略运行时
     * 
     * @param runtime 策略运行时
     * @throws IllegalArgumentException 如果runtime为null
     */
    public void register(StrategyRuntime runtime) {
        if (runtime == null) {
            throw new IllegalArgumentException("策略运行时不能为null");
        }

        if (runtime.getStrategyId() == null) {
            throw new IllegalArgumentException("策略ID不能为null");
        }

        StrategyRuntime existing = runtimes.put(runtime.getStrategyId(), runtime);
        if (existing != null) {
            log.warn("策略运行时已存在，将被替换: strategyId={}", runtime.getStrategyId());
        } else {
            log.info("策略运行时注册成功: strategyId={}, instanceCount={}",
                runtime.getStrategyId(), runtime.getInstanceCount());
        }
    }

    /**
     * 注销策略运行时
     * 
     * @param strategyId 策略ID
     * @return 被注销的运行时，如果不存在返回null
     */
    public StrategyRuntime unregister(Long strategyId) {
        if (strategyId == null) {
            return null;
        }

        StrategyRuntime removed = runtimes.remove(strategyId);
        if (removed != null) {
            log.info("策略运行时注销成功: strategyId={}", strategyId);
        } else {
            log.debug("策略运行时不存在，无需注销: strategyId={}", strategyId);
        }

        return removed;
    }

    /**
     * 根据策略ID查询运行时
     * 
     * @param strategyId 策略ID
     * @return 策略运行时，如果不存在返回null
     */
    public StrategyRuntime getRuntime(Long strategyId) {
        if (strategyId == null) {
            return null;
        }
        return runtimes.get(strategyId);
    }

    /**
     * 查询所有运行时
     * 
     * @return 所有运行时列表
     */
    public Collection<StrategyRuntime> getAllRuntimes() {
        return runtimes.values();
    }

    /**
     * 获取运行时数量
     * 
     * @return 运行时数量
     */
    public int getRuntimeCount() {
        return runtimes.size();
    }

    /**
     * 检查策略是否已注册
     * 
     * @param strategyId 策略ID
     * @return true如果已注册，false否则
     */
    public boolean isRegistered(Long strategyId) {
        if (strategyId == null) {
            return false;
        }
        return runtimes.containsKey(strategyId);
    }

    /**
     * 清空所有运行时（主要用于测试）
     */
    public void clear() {
        int count = runtimes.size();
        runtimes.clear();
        log.info("清空所有策略运行时: count={}", count);
    }
}

