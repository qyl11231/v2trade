package com.qyl.v2trade.indicator.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 指标值DTO（用于API响应）
 *
 * @author qyl
 */
public record IndicatorValueDTO(
    /**
     * 指标值ID
     */
    Long id,
    
    /**
     * 用户ID
     */
    Long userId,
    
    /**
     * 交易对ID
     */
    Long tradingPairId,
    
    /**
     * 交易对符号
     */
    String symbol,
    
    /**
     * 市场类型
     */
    String marketType,
    
    /**
     * 周期
     */
    String timeframe,
    
    /**
     * Bar收盘时间（UTC）
     */
    LocalDateTime barTime,
    
    /**
     * 指标编码
     */
    String indicatorCode,
    
    /**
     * 指标版本
     */
    String indicatorVersion,
    
    /**
     * 单值指标结果
     */
    BigDecimal value,
    
    /**
     * 多值指标结果
     */
    Map<String, BigDecimal> extraValues,
    
    /**
     * 数据质量
     */
    String dataQuality,
    
    /**
     * 计算引擎
     */
    String calcEngine,
    
    /**
     * 计算耗时（毫秒）
     */
    Integer calcCostMs,
    
    /**
     * 创建时间
     */
    LocalDateTime createdAt
) {
    /**
     * 从Entity转换为DTO
     */
    public static IndicatorValueDTO fromEntity(com.qyl.v2trade.indicator.repository.entity.IndicatorValue entity) {
        if (entity == null) {
            return null;
        }
        
        return new IndicatorValueDTO(
                entity.getId(),
                entity.getUserId(),
                entity.getTradingPairId(),
                entity.getSymbol(),
                entity.getMarketType(),
                entity.getTimeframe(),
                entity.getBarTime(),
                entity.getIndicatorCode(),
                entity.getIndicatorVersion(),
                entity.getValue(),
                entity.getExtraValues(),
                entity.getDataQuality(),
                entity.getCalcEngine(),
                entity.getCalcCostMs(),
                entity.getCreatedAt()
        );
    }
}

