package com.qyl.v2trade.business.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 策略参数实体
 */
@Data
@TableName("strategy_param")
public class StrategyParam implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 参数ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 策略ID（唯一）
     */
    private Long strategyId;

    /**
     * 策略初始虚拟资金（用于内部收益曲线计算）
     */
    private BigDecimal initialCapital;

    /**
     * 单次下单资金占比
     */
    private BigDecimal baseOrderRatio;

    /**
     * 策略止盈比例（兜底型）
     */
    private BigDecimal takeProfitRatio;

    /**
     * 策略止损比例（兜底型）
     */
    private BigDecimal stopLossRatio;

    /**
     * 策略入场条件（JSON格式）
     */
    private String entryCondition;

    /**
     * 策略退出条件（JSON格式）
     */
    private String exitCondition;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

