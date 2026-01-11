package com.qyl.v2trade.business.strategy.decision.logic.condition;

import lombok.Builder;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 条件树节点
 * 
 * <p>表示一个条件组，可以包含规则和嵌套组
 */
@Getter
@Builder
public class ConditionTree implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 版本号
     */
    private final String version;

    /**
     * 组合模式：ALL / ANY
     */
    private final String mode;

    /**
     * 规则列表
     */
    private final List<ConditionRule> rules;

    /**
     * 嵌套组列表（支持递归嵌套）
     */
    private final List<ConditionTree> groups;
}

