package com.qyl.v2trade.market.calibration.log.dto;

import lombok.Data;

/**
 * 更新任务执行日志请求DTO
 */
@Data
public class TaskLogUpdateRequest {

    /**
     * 执行状态
     */
    private String status;

    /**
     * 缺失K线数量
     */
    private Integer missingCount;

    /**
     * 补全K线数量
     */
    private Integer filledCount;

    /**
     * 重复数据数量
     */
    private Integer duplicateCount;

    /**
     * 异常数据数量
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

