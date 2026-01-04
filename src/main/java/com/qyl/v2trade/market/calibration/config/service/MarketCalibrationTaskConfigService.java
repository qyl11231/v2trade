package com.qyl.v2trade.market.calibration.config.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.qyl.v2trade.market.calibration.config.dto.TaskConfigCreateRequest;
import com.qyl.v2trade.market.calibration.config.dto.TaskConfigQueryRequest;
import com.qyl.v2trade.market.calibration.config.dto.TaskConfigUpdateRequest;
import com.qyl.v2trade.market.calibration.config.dto.TaskConfigVO;
import com.qyl.v2trade.market.calibration.config.entity.MarketCalibrationTaskConfig;

/**
 * 行情校准任务配置服务接口
 */
public interface MarketCalibrationTaskConfigService extends IService<MarketCalibrationTaskConfig> {

    /**
     * 创建任务配置
     * @param request 创建请求
     * @return 创建的任务配置
     */
    TaskConfigVO createTaskConfig(TaskConfigCreateRequest request);

    /**
     * 更新任务配置
     * @param id 任务配置ID
     * @param request 更新请求
     * @return 更新后的任务配置
     */
    TaskConfigVO updateTaskConfig(Long id, TaskConfigUpdateRequest request);

    /**
     * 删除任务配置
     * @param id 任务配置ID
     */
    void deleteTaskConfig(Long id);

    /**
     * 根据ID查询任务配置
     * @param id 任务配置ID
     * @return 任务配置VO
     */
    TaskConfigVO getTaskConfigById(Long id);

    /**
     * 查询任务配置列表
     * @param request 查询请求
     * @return 分页结果
     */
    IPage<TaskConfigVO> listTaskConfigs(TaskConfigQueryRequest request);

    /**
     * 启用/禁用任务
     * @param id 任务配置ID
     * @param enabled 是否启用：1-启用 0-禁用
     * @return 更新后的任务配置
     */
    TaskConfigVO toggleEnabled(Long id, Boolean enabled);

    /**
     * 查询所有启用的自动任务配置
     * @param taskType 任务类型（可选，如果为null则查询所有类型）
     * @return 任务配置列表
     */
    java.util.List<MarketCalibrationTaskConfig> listEnabledAutoTasks(String taskType);
}

