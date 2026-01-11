package com.qyl.v2trade.common.constants;

/**
 * 策略决策意图动作枚举
 * 
 * <p>用于 strategy_intent_record 表的 intent_action 字段
 * 
 * <p>决策意图说明：
 * <ul>
 *   <li>OPEN: 开仓（做多或做空）</li>
 *   <li>CLOSE: 平仓（平掉当前持仓）</li>
 *   <li>ADD: 加仓（在现有持仓基础上增加）</li>
 *   <li>REDUCE: 减仓（部分平仓）</li>
 *   <li>REVERSE: 反手（平掉当前持仓并反向开仓）</li>
 *   <li>HOLD: 不动作（不落库，只记录metrics/log）</li>
 * </ul>
 */
public enum IntentActionEnum {
    
    /**
     * 开仓（做多或做空）
     */
    OPEN("OPEN", "开仓"),
    
    /**
     * 平仓（平掉当前持仓）
     */
    CLOSE("CLOSE", "平仓"),
    
    /**
     * 加仓（在现有持仓基础上增加）
     */
    ADD("ADD", "加仓"),
    
    /**
     * 减仓（部分平仓）
     */
    REDUCE("REDUCE", "减仓"),
    
    /**
     * 反手（平掉当前持仓并反向开仓）
     */
    REVERSE("REVERSE", "反手"),
    
    /**
     * 不动作（不落库，只记录metrics/log）
     * 
     * <p>阶段2约束：HOLD不写入strategy_intent_record表
     */
    HOLD("HOLD", "不动作");

    private final String code;
    private final String description;

    IntentActionEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据代码获取枚举
     * 
     * @param code 代码
     * @return 枚举值
     * @throws IllegalArgumentException 如果代码不存在
     */
    public static IntentActionEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (IntentActionEnum action : values()) {
            if (action.code.equals(code)) {
                return action;
            }
        }
        throw new IllegalArgumentException("Unknown intent action code: " + code);
    }

    /**
     * 判断是否为动作意图（需要落库）
     * 
     * <p>阶段2约束：只有动作意图才写入strategy_intent_record表
     * 
     * @return true如果是动作意图，false如果是HOLD
     */
    public boolean isActionIntent() {
        return this != HOLD;
    }
}

