package com.qyl.v2trade.market.calibration.log.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 行情校准任务执行日志实体
 * 用于记录任务执行历史
 */
@Data
@TableName("market_calibration_task_log")
public class MarketCalibrationTaskLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务配置ID
     */
    private Long taskConfigId;

    /**
     * 任务名称（冗余字段，便于查询）
     */
    private String taskName;

    /**
     * 任务类型
     */
    private String taskType;

    /**
     * 交易对ID
     */
    private Long tradingPairId;

    /**
     * 交易对符号（冗余字段）
     */
    private String symbol;

    /**
     * 执行模式
     */
    private String executionMode;

    /**
     * 检测开始时间
     */
    private LocalDateTime detectStartTime;

    /**
     * 检测结束时间
     */
    private LocalDateTime detectEndTime;

    /**
     * 执行状态：RUNNING-执行中, SUCCESS-成功, FAILED-失败
     */
    private String status;

    /**
     * 缺失K线数量（仅缺失检测任务）
     */
    private Integer missingCount;

    /**
     * 补全K线数量（仅缺失检测任务）
     */
    private Integer filledCount;

    /**
     * 重复数据数量（仅核对任务）
     */
    private Integer duplicateCount;

    /**
     * 异常数据数量（仅核对任务）
     */
    private Integer errorCount;

    /**
     * 执行耗时（毫秒）
     */
    private Long executeDurationMs;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行日志详情（JSON格式）
     */
    private String executeLog;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

