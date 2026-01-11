package com.qyl.v2trade.business.strategy.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.qyl.v2trade.common.constants.IntentActionEnum;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 策略决策记录实体
 * 
 * <p>映射 strategy_intent_record 表
 * 
 * <p>职责：
 * <ul>
 *   <li>记录策略在某一时刻的决策意图（"我决定做什么"）</li>
 *   <li>只写一次，不回滚，不覆盖（阶段2约束）</li>
 *   <li>用于下游阶段3执行和审计回放</li>
 * </ul>
 * 
 * <p>阶段2约束：
 * <ul>
 *   <li>只INSERT，不允许UPDATE/DELETE</li>
 *   <li>只有动作意图才落库（OPEN/CLOSE/ADD/REDUCE/REVERSE）</li>
 *   <li>HOLD不落库（只记录metrics/log）</li>
 *   <li>decision_reason必须是结构化JSON</li>
 * </ul>
 */
@Data
@TableName("strategy_intent_record")
public class StrategyIntentRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 决策记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 策略ID
     */
    private Long strategyId;

    /**
     * 交易对ID
     */
    private Long tradingPairId;

    /**
     * 触发决策的信号ID（可空）
     * 
     * <p>如果决策由信号触发，记录signal_intent.id
     */
    private Long signalId;

    /**
     * 决策意图：OPEN / CLOSE / ADD / REDUCE / REVERSE / HOLD
     * 
     * <p>注意：HOLD不落库，所以表中不会出现HOLD记录
     */
    private String intentAction;

    /**
     * 策略计算出的下单数量
     * 
     * <p>基于策略参数（initial_capital * base_order_ratio）计算
     */
    private BigDecimal calculatedQty;

    /**
     * 决策原因说明（结构化JSON）
     * 
     * <p>必须包含：
     * <ul>
     *   <li>trigger: 触发源信息</li>
     *   <li>logicStateBefore: 决策前的逻辑状态</li>
     *   <li>snapshots: 数据快照（signal/indicator/bar/price）</li>
     *   <li>paramsDigest: 参数摘要（可选）</li>
     * </ul>
     * 
     * <p>用于回放和审计
     */
    private String decisionReason;

    /**
     * 决策时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 获取决策意图枚举
     * 
     * @return 决策意图枚举，如果为null返回null
     */
    public IntentActionEnum getIntentActionEnum() {
        if (intentAction == null) {
            return null;
        }
        return IntentActionEnum.fromCode(intentAction);
    }

    /**
     * 设置决策意图枚举
     * 
     * @param action 决策意图枚举
     */
    public void setIntentActionEnum(IntentActionEnum action) {
        if (action == null) {
            this.intentAction = null;
        } else {
            this.intentAction = action.getCode();
        }
    }
}

