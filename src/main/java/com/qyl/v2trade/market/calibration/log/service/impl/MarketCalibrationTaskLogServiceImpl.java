package com.qyl.v2trade.market.calibration.log.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qyl.v2trade.exception.BusinessException;
import com.qyl.v2trade.market.calibration.log.dto.TaskLogCreateRequest;
import com.qyl.v2trade.market.calibration.log.dto.TaskLogUpdateRequest;
import com.qyl.v2trade.market.calibration.log.entity.MarketCalibrationTaskLog;
import com.qyl.v2trade.market.calibration.log.mapper.MarketCalibrationTaskLogMapper;
import com.qyl.v2trade.market.calibration.log.service.MarketCalibrationTaskLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 行情校准任务执行日志服务实现类
 */
@Service
public class MarketCalibrationTaskLogServiceImpl extends ServiceImpl<MarketCalibrationTaskLogMapper, MarketCalibrationTaskLog>
        implements MarketCalibrationTaskLogService {

    @Override
    public MarketCalibrationTaskLog getLatestLogByTaskConfigId(Long taskConfigId) {
        if (taskConfigId == null) {
            return null;
        }
        return baseMapper.selectLatestByTaskConfigId(taskConfigId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketCalibrationTaskLog createLog(TaskLogCreateRequest request) {
        if (request == null) {
            throw new BusinessException("创建请求不能为空");
        }

        MarketCalibrationTaskLog log = new MarketCalibrationTaskLog();
        log.setTaskConfigId(request.getTaskConfigId());
        log.setTaskName(request.getTaskName());
        log.setTaskType(request.getTaskType());
        log.setTradingPairId(request.getTradingPairId());
        log.setSymbol(request.getSymbol());
        log.setExecutionMode(request.getExecutionMode());
        log.setDetectStartTime(request.getDetectStartTime());
        log.setDetectEndTime(request.getDetectEndTime());
        log.setStatus(request.getStatus());
        log.setMissingCount(0);
        log.setFilledCount(0);
        log.setDuplicateCount(0);
        log.setErrorCount(0);

        save(log);
        return log;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketCalibrationTaskLog updateLogStatus(Long logId, String status, TaskLogUpdateRequest request) {
        if (logId == null) {
            throw new BusinessException("日志ID不能为空");
        }

        MarketCalibrationTaskLog log = getById(logId);
        if (log == null) {
            throw new BusinessException("执行日志不存在");
        }

        if (status != null) {
            log.setStatus(status);
        }

        if (request != null) {
            if (request.getMissingCount() != null) {
                log.setMissingCount(request.getMissingCount());
            }
            if (request.getFilledCount() != null) {
                log.setFilledCount(request.getFilledCount());
            }
            if (request.getDuplicateCount() != null) {
                log.setDuplicateCount(request.getDuplicateCount());
            }
            if (request.getErrorCount() != null) {
                log.setErrorCount(request.getErrorCount());
            }
            if (request.getExecuteDurationMs() != null) {
                log.setExecuteDurationMs(request.getExecuteDurationMs());
            }
            if (request.getErrorMessage() != null) {
                log.setErrorMessage(request.getErrorMessage());
            }
            if (request.getExecuteLog() != null) {
                log.setExecuteLog(request.getExecuteLog());
            }
        }

        updateById(log);
        return log;
    }

    @Override
    public boolean existsRunningLog(Long taskConfigId) {
        if (taskConfigId == null) {
            return false;
        }
        int count = baseMapper.countRunningLogs(taskConfigId);
        return count > 0;
    }
}

