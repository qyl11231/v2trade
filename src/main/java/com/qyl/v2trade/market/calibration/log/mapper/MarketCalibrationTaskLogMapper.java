package com.qyl.v2trade.market.calibration.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qyl.v2trade.market.calibration.log.entity.MarketCalibrationTaskLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 行情校准任务执行日志Mapper接口
 */
@Mapper
public interface MarketCalibrationTaskLogMapper extends BaseMapper<MarketCalibrationTaskLog> {

    /**
     * 查询任务配置的最新执行记录
     * @param taskConfigId 任务配置ID
     * @return 最新执行记录
     */
    @Select("SELECT * FROM market_calibration_task_log " +
            "WHERE task_config_id = #{taskConfigId} " +
            "ORDER BY created_at DESC " +
            "LIMIT 1")
    MarketCalibrationTaskLog selectLatestByTaskConfigId(@Param("taskConfigId") Long taskConfigId);

    /**
     * 检查指定任务配置是否有正在执行的任务
     * @param taskConfigId 任务配置ID
     * @return 正在执行的任务数量
     */
    @Select("SELECT COUNT(*) FROM market_calibration_task_log " +
            "WHERE task_config_id = #{taskConfigId} " +
            "AND status = 'RUNNING'")
    int countRunningLogs(@Param("taskConfigId") Long taskConfigId);
}

