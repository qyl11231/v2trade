package com.qyl.v2trade.market.calibration.log.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qyl.v2trade.market.calibration.log.dto.TaskLogCreateRequest;
import com.qyl.v2trade.market.calibration.log.dto.TaskLogUpdateRequest;
import com.qyl.v2trade.market.calibration.log.entity.MarketCalibrationTaskLog;

/**
 * 行情校准任务执行日志服务接口
 */
public interface MarketCalibrationTaskLogService extends IService<MarketCalibrationTaskLog> {

    /**
     * 查询任务配置的最新执行记录
     * @param taskConfigId 任务配置ID
     * @return 最新执行记录，未找到返回null
     */
    MarketCalibrationTaskLog getLatestLogByTaskConfigId(Long taskConfigId);

    /**
     * 创建执行日志
     * @param request 创建请求
     * @return 创建的日志记录
     */
    MarketCalibrationTaskLog createLog(TaskLogCreateRequest request);

    /**
     * 更新执行日志
     * @param logId 日志ID
     * @param status 状态
     * @param request 更新请求
     * @return 更新后的日志记录
     */
    MarketCalibrationTaskLog updateLogStatus(Long logId, String status, TaskLogUpdateRequest request);

    /**
     * 检查指定任务配置是否有正在执行的任务
     * @param taskConfigId 任务配置ID
     * @return true表示有正在执行的任务，false表示没有
     */
    boolean existsRunningLog(Long taskConfigId);
}

