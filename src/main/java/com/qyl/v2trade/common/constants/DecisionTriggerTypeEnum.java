package com.qyl.v2trade.common.constants;

/**
 * 决策触发类型枚举
 * 
 * <p>用于标识决策是由哪种触发源引起的
 * 
 * <p>触发类型说明：
 * <ul>
 *   <li>SIGNAL: 信号意图激活（来自signal_intent表）</li>
 *   <li>INDICATOR: 指标计算完成（来自indicator_value表）</li>
 *   <li>BAR: K线闭合（来自BarClosedEvent）</li>
 *   <li>PRICE: 价格阈值穿越（止盈止损/突破价位）</li>
 * </ul>
 */
public enum DecisionTriggerTypeEnum {
    
    /**
     * 信号意图激活
     * 
     * <p>外部信号源（如TradingView）产生的信号意图
     */
    SIGNAL("SIGNAL", "信号意图激活"),
    
    /**
     * 指标计算完成
     * 
     * <p>技术指标计算完成，指标值更新
     */
    INDICATOR("INDICATOR", "指标计算完成"),
    
    /**
     * K线闭合
     * 
     * <p>一根K线完成，可以用于形态确认
     */
    BAR("BAR", "K线闭合"),
    
    /**
     * 价格阈值穿越
     * 
     * <p>价格穿越止盈止损阈值或突破价位
     */
    PRICE("PRICE", "价格阈值穿越");

    private final String code;
    private final String description;

    DecisionTriggerTypeEnum(String code, String description) {
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
    public static DecisionTriggerTypeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (DecisionTriggerTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown decision trigger type code: " + code);
    }
}

