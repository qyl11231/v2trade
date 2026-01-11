package com.qyl.v2trade.business.strategy.decision.logic.condition;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qyl.v2trade.business.strategy.decision.context.DecisionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 条件评估器（重构版）
 * 
 * <p>职责：
 * <ul>
 *   <li>解析JSON为结构化规则树（AST）</li>
 *   <li>支持ALL/ANY模式和嵌套组</li>
 *   <li>输出完整的hitReason（用于审计）</li>
 *   <li>支持多种因子类型（信号/指标/K线/价格/状态）</li>
 * </ul>
 * 
 * <p>设计原则：
 * <ul>
 *   <li>结构化规则树（不执行任意代码）</li>
 *   <li>白名单机制（factor key、operator都有白名单）</li>
 *   <li>类型安全（强类型检查）</li>
 *   <li>可审计（完整hitReason）</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConditionEvaluator {

    private final FactorResolverRegistry factorResolverRegistry;
    private final OperatorExecutor operatorExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 评估条件（主入口）
     * 
     * @param ctx 决策上下文
     * @param conditionJson 条件JSON字符串
     * @return 评估结果（包含passed、blocked、hitReason）
     */
    public EvaluationResult evaluate(DecisionContext ctx, String conditionJson) {
        if (conditionJson == null || conditionJson.isEmpty()) {
            return EvaluationResult.blocked("条件JSON为空");
        }

        try {
            // 1. 解析JSON为条件树
            ConditionTree tree = parseConditionTree(conditionJson);

            // 2. 评估条件树
            return evaluateTree(ctx, tree);

        } catch (Exception e) {
            log.error("条件评估失败: conditionJson={}", conditionJson, e);
            return EvaluationResult.blocked("条件解析失败: " + e.getMessage());
        }
    }

    /**
     * 解析条件树
     */
    private ConditionTree parseConditionTree(String conditionJson) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> json = objectMapper.readValue(conditionJson, Map.class);

        // 解析规则
        List<ConditionRule> rules = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rulesJson = (List<Map<String, Object>>) json.get("rules");
        if (rulesJson != null) {
            for (Map<String, Object> ruleJson : rulesJson) {
                ConditionRule rule = ConditionRule.builder()
                    .factor((String) ruleJson.get("factor"))
                    .operator((String) ruleJson.get("operator"))
                    .value(ruleJson.get("value"))
                    .type((String) ruleJson.getOrDefault("type", "NUMBER"))
                    .nullable((Boolean) ruleJson.getOrDefault("nullable", false))
                    .build();
                rules.add(rule);
            }
        }

        // 解析嵌套组（递归）
        List<ConditionTree> groups = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> groupsJson = (List<Map<String, Object>>) json.get("groups");
        if (groupsJson != null) {
            for (Map<String, Object> groupJson : groupsJson) {
                ConditionTree group = parseConditionTree(objectMapper.writeValueAsString(groupJson));
                groups.add(group);
            }
        }

        return ConditionTree.builder()
            .version((String) json.getOrDefault("version", "1.0"))
            .mode((String) json.getOrDefault("mode", "ALL"))
            .rules(rules)
            .groups(groups)
            .build();
    }

    /**
     * 评估条件树（递归）
     */
    private EvaluationResult evaluateTree(DecisionContext ctx, ConditionTree tree) {
        List<HitReason.RuleHit> ruleHits = new ArrayList<>();
        List<HitReason.GroupHit> groupHits = new ArrayList<>();

        // 1. 评估规则
        List<Boolean> ruleResults = new ArrayList<>();
        for (ConditionRule rule : tree.getRules()) {
            EvaluationResult ruleResult = evaluateRule(ctx, rule);
            ruleHits.add(HitReason.RuleHit.builder()
                .factor(rule.getFactor())
                .operator(rule.getOperator())
                .value(rule.getValue())
                .hit(ruleResult.isPassed())
                .reason(ruleResult.isBlocked() ? ruleResult.getBlockReason() :
                       (ruleResult.isPassed() ? "命中" : "未命中"))
                .build());

            if (ruleResult.isBlocked()) {
                // 缺值且nullable=false，直接返回blocked
                return EvaluationResult.blocked(ruleResult.getBlockReason());
            }

            ruleResults.add(ruleResult.isPassed());

            // ANY模式短路：找到第一个true即返回
            if ("ANY".equals(tree.getMode()) && ruleResult.isPassed()) {
                return EvaluationResult.passed(HitReason.builder()
                    .ruleHits(ruleHits)
                    .groupHits(groupHits)
                    .build());
            }
        }

        // 2. 评估嵌套组
        List<Boolean> groupResults = new ArrayList<>();
        for (ConditionTree group : tree.getGroups()) {
            EvaluationResult groupResult = evaluateTree(ctx, group);
            if (groupResult.isBlocked()) {
                return groupResult;  // 组被block，直接返回
            }

            groupHits.add(HitReason.GroupHit.builder()
                .mode(group.getMode())
                .hit(groupResult.isPassed())
                .ruleHits(groupResult.getHitReason() != null ?
                    groupResult.getHitReason().getRuleHits() : List.of())
                .build());

            groupResults.add(groupResult.isPassed());

            // ANY模式短路
            if ("ANY".equals(tree.getMode()) && groupResult.isPassed()) {
                return EvaluationResult.passed(HitReason.builder()
                    .ruleHits(ruleHits)
                    .groupHits(groupHits)
                    .build());
            }
        }

        // 3. 组合结果
        boolean passed;
        if ("ANY".equals(tree.getMode())) {
            // ANY模式：规则或组任一满足即可
            passed = ruleResults.stream().anyMatch(b -> b) ||
                    groupResults.stream().anyMatch(b -> b);
        } else {
            // ALL模式：所有规则和组都必须满足
            passed = ruleResults.stream().allMatch(b -> b) &&
                    groupResults.stream().allMatch(b -> b);
        }

        HitReason hitReason = HitReason.builder()
            .ruleHits(ruleHits)
            .groupHits(groupHits)
            .build();

        return passed ? EvaluationResult.passed(hitReason) : EvaluationResult.failed(hitReason);
    }

    /**
     * 评估单个规则
     */
    private EvaluationResult evaluateRule(DecisionContext ctx, ConditionRule rule) {
        // 1. 白名单校验
        if (!factorResolverRegistry.isFactorKeyAllowed(rule.getFactor())) {
            return EvaluationResult.blocked("因子key不在白名单: " + rule.getFactor());
        }

        // 2. 解析左值（factor）
        Optional<TypedValue> leftValueOpt = factorResolverRegistry.resolve(rule.getFactor(), ctx);
        if (leftValueOpt.isEmpty()) {
            // 值缺失
            if (Boolean.TRUE.equals(rule.getNullable())) {
                return EvaluationResult.failed(HitReason.builder()
                    .ruleHits(List.of(HitReason.RuleHit.builder()
                        .factor(rule.getFactor())
                        .operator(rule.getOperator())
                        .value(rule.getValue())
                        .hit(false)
                        .reason("因子值缺失（nullable=true）")
                        .build()))
                    .build());
            } else {
                return EvaluationResult.blocked("因子值缺失且nullable=false: " + rule.getFactor());
            }
        }

        // 3. 解析右值（value，可能是字面量或因子引用）
        TypedValue rightValue = resolveRightValue(ctx, rule.getValue(), rule.getType());
        if (rightValue == null) {
            return EvaluationResult.blocked("右值解析失败: " + rule.getValue());
        }

        // 4. 执行比较
        try {
            Operator operator = Operator.fromString(rule.getOperator());
            boolean result = operatorExecutor.execute(leftValueOpt.get(), operator, rightValue);

            return result ?
                EvaluationResult.passed(HitReason.builder()
                    .ruleHits(List.of(HitReason.RuleHit.builder()
                        .factor(rule.getFactor())
                        .operator(rule.getOperator())
                        .value(rule.getValue())
                        .hit(true)
                        .reason("命中")
                        .build()))
                    .build()) :
                EvaluationResult.failed(HitReason.builder()
                    .ruleHits(List.of(HitReason.RuleHit.builder()
                        .factor(rule.getFactor())
                        .operator(rule.getOperator())
                        .value(rule.getValue())
                        .hit(false)
                        .reason("未命中")
                        .build()))
                    .build());

        } catch (Exception e) {
            log.error("规则评估异常: rule={}", rule, e);
            return EvaluationResult.blocked("规则评估异常: " + e.getMessage());
        }
    }

    /**
     * 解析右值（支持字面量和因子引用）
     */
    private TypedValue resolveRightValue(DecisionContext ctx, Object value, String type) {
        if (value == null) {
            return null;
        }

        // 如果是因子引用（如 "STATE.STOP_LOSS_PRICE"）
        if (value instanceof String && ((String) value).contains(".")) {
            String valueStr = (String) value;
            if (factorResolverRegistry.isFactorKeyAllowed(valueStr)) {
                Optional<TypedValue> factorValue = factorResolverRegistry.resolve(valueStr, ctx);
                return factorValue.orElse(null);
            }
        }

        // 字面量
        switch (type) {
            case "NUMBER":
                if (value instanceof Number) {
                    return TypedValue.ofNumber(BigDecimal.valueOf(((Number) value).doubleValue()));
                } else if (value instanceof String) {
                    try {
                        return TypedValue.ofNumber(new BigDecimal((String) value));
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
                break;
            case "STRING":
                return TypedValue.ofString(String.valueOf(value));
            case "BOOLEAN":
                if (value instanceof Boolean) {
                    return TypedValue.ofBoolean((Boolean) value);
                } else if (value instanceof String) {
                    return TypedValue.ofBoolean(Boolean.parseBoolean((String) value));
                }
                break;
        }

        return null;
    }
}

