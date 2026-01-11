package com.qyl.v2trade.business.strategy.decision.logic.condition;

/**
 * 操作符枚举
 * 
 * <p>支持的操作符：
 * <ul>
 *   <li>数值比较：GT, LT, GTE, LTE, EQ, NEQ</li>
 *   <li>范围比较：BETWEEN, NOT_BETWEEN</li>
 *   <li>字符串比较：CONTAINS, STARTS_WITH, ENDS_WITH</li>
 * </ul>
 */
public enum Operator {
    // 数值比较
    GT("大于"),
    LT("小于"),
    GTE("大于等于"),
    LTE("小于等于"),
    EQ("等于"),
    NEQ("不等于"),
    
    // 范围比较
    BETWEEN("在范围内"),
    NOT_BETWEEN("不在范围内"),
    
    // 字符串比较
    CONTAINS("包含"),
    STARTS_WITH("以...开头"),
    ENDS_WITH("以...结尾");
    
    private final String description;

    Operator(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 从字符串解析
     * 
     * @param op 操作符字符串
     * @return 操作符枚举
     * @throws IllegalArgumentException 如果操作符不支持
     */
    public static Operator fromString(String op) {
        if (op == null) {
            throw new IllegalArgumentException("操作符不能为null");
        }
        try {
            return valueOf(op.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("不支持的操作符: " + op);
        }
    }
}

