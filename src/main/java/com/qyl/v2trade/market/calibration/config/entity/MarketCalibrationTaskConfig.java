package com.qyl.v2trade.market.calibration.config.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 行情校准任务配置实体
 * 用于管理行情校准任务配置
 */
@Data
@TableName("market_calibration_task_config")
public class MarketCalibrationTaskConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务类型：MISSING_DATA-缺失数据检测, DATA_VERIFY-数据核对
     */
    private String taskType;

    /**
     * 交易对ID（关联trading_pair表）
     */
    private Long tradingPairId;

    /**
     * 执行模式：AUTO-自动, MANUAL-手动
     */
    private String executionMode;

    /**
     * 自动模式：检测周期（小时），如1表示检测最近1小时
     */
    private Integer intervalHours;

    /**
     * 手动模式：开始时间
     */
    private LocalDateTime startTime;

    /**
     * 手动模式：结束时间
     */
    private LocalDateTime endTime;

    /**
     * 是否启用：1-启用 0-禁用
     */
    private Integer enabled;

    /**
     * 备注说明
     */
    private String remark;

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

