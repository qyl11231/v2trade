package com.qyl.v2trade.market.calibration.executor.dto;

import lombok.Data;

/**
 * 任务执行结果DTO
 */
@Data
public class TaskExecutionResult {

    /**
     * 是否成功
     */
    private boolean success;

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
}

