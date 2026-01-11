package com.qyl.v2trade.business.strategy.decision.logic.condition;

import com.qyl.v2trade.business.strategy.decision.context.DecisionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 因子解析器注册表
 * 
 * <p>职责：
 * <ul>
 *   <li>管理所有FactorResolver实现</li>
 *   <li>根据factor key的前缀路由到对应的解析器</li>
 *   <li>提供白名单校验</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FactorResolverRegistry {

    /**
     * 所有FactorResolver实现（由Spring自动注入）
     */
    private final List<FactorResolver> resolvers;

    /**
     * 解析器映射（prefix -> resolver）
     */
    private final Map<String, FactorResolver> resolverMap = new HashMap<>();

    /**
     * 初始化注册表
     */
    @PostConstruct
    public void init() {
        for (FactorResolver resolver : resolvers) {
            String prefix = resolver.getSupportedPrefix();
            FactorResolver existing = resolverMap.put(prefix, resolver);
            if (existing != null) {
                log.warn("因子解析器重复注册，将被覆盖: prefix={}, existing={}, new={}",
                    prefix, existing.getClass().getName(), resolver.getClass().getName());
            } else {
                log.info("因子解析器注册成功: prefix={}, implementation={}",
                    prefix, resolver.getClass().getName());
            }
        }
        log.info("因子解析器注册表初始化完成: registeredPrefixes={}", resolverMap.keySet());
    }

    /**
     * 解析因子值
     * 
     * @param factorKey 因子key（如 "IND.RSI_14"）
     * @param ctx 决策上下文
     * @return 因子值（如果不存在返回Optional.empty()）
     */
    public Optional<TypedValue> resolve(String factorKey, DecisionContext ctx) {
        if (factorKey == null || factorKey.isEmpty()) {
            return Optional.empty();
        }

        // 查找匹配的解析器
        for (Map.Entry<String, FactorResolver> entry : resolverMap.entrySet()) {
            if (factorKey.startsWith(entry.getKey())) {
                return entry.getValue().resolve(factorKey, ctx);
            }
        }

        log.debug("未找到匹配的因子解析器: factorKey={}", factorKey);
        return Optional.empty();
    }

    /**
     * 校验因子key是否在白名单中
     * 
     * @param factorKey 因子key
     * @return true如果在白名单中，false否则
     */
    public boolean isFactorKeyAllowed(String factorKey) {
        if (factorKey == null || factorKey.isEmpty()) {
            return false;
        }
        return resolverMap.keySet().stream()
            .anyMatch(factorKey::startsWith);
    }
}

