package com.qyl.v2trade.business.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 策略完整创建请求DTO（包含所有配置）
 */
@Data
public class StrategyCreateRequest {

    /**
     * 策略定义
     */
    @Valid
    @NotNull(message = "策略定义不能为空")
    private StrategyDefinitionPart definition;

    /**
     * 策略参数
     */
    @Valid
    @NotNull(message = "策略参数不能为空")
    private StrategyParamPart param;

    /**
     * 交易对配置列表
     */
    private List<TradingPairPart> tradingPairs;

    /**
     * 信号订阅配置列表
     */
    private List<SignalSubscriptionPart> signalSubscriptions;

    /**
     * 策略定义部分
     */
    @Data
    public static class StrategyDefinitionPart {
        @NotBlank(message = "策略名称不能为空")
        private String strategyName;

        @NotBlank(message = "策略类型不能为空")
        private String strategyType;

        private String decisionMode;

        private Integer enabled;
    }

    /**
     * 策略参数部分
     */
    @Data
    public static class StrategyParamPart {
        @NotNull(message = "初始资金不能为空")
        private BigDecimal initialCapital;

        @NotNull(message = "单次下单比例不能为空")
        private BigDecimal baseOrderRatio;

        private BigDecimal takeProfitRatio;

        private BigDecimal stopLossRatio;

        private String entryCondition;

        private String exitCondition;
    }

    /**
     * 交易对配置部分
     */
    @Data
    public static class TradingPairPart {
        /**
         * 策略交易对ID（更新时使用，新增时为null）
         */
        private Long id;

        @NotNull(message = "交易对ID不能为空")
        private Long tradingPairId;

        private Integer enabled;
    }

    /**
     * 信号订阅配置部分
     */
    @Data
    public static class SignalSubscriptionPart {
        /**
         * 策略信号订阅ID（更新时使用，新增时为null）
         */
        private Long id;

        @NotNull(message = "信号配置ID不能为空")
        private Long signalConfigId;

        private String consumeMode;

        private Integer enabled;
    }
}

