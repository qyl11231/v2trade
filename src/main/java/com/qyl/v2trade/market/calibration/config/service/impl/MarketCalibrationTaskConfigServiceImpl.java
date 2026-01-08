package com.qyl.v2trade.market.calibration.config.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qyl.v2trade.business.system.model.entity.TradingPair;
import com.qyl.v2trade.business.system.service.TradingPairService;
import com.qyl.v2trade.common.constants.EnabledStatus;
import com.qyl.v2trade.exception.BusinessException;
import com.qyl.v2trade.market.calibration.common.ExecutionMode;
import com.qyl.v2trade.market.calibration.common.TaskType;
import com.qyl.v2trade.market.calibration.config.dto.TaskConfigCreateRequest;
import com.qyl.v2trade.market.calibration.config.dto.TaskConfigQueryRequest;
import com.qyl.v2trade.market.calibration.config.dto.TaskConfigUpdateRequest;
import com.qyl.v2trade.market.calibration.config.dto.TaskConfigVO;
import com.qyl.v2trade.market.calibration.config.entity.MarketCalibrationTaskConfig;
import com.qyl.v2trade.market.calibration.config.mapper.MarketCalibrationTaskConfigMapper;
import com.qyl.v2trade.market.calibration.config.service.MarketCalibrationTaskConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/**
 * 行情校准任务配置服务实现类
 */
@Service
public class MarketCalibrationTaskConfigServiceImpl extends ServiceImpl<MarketCalibrationTaskConfigMapper, MarketCalibrationTaskConfig>
        implements MarketCalibrationTaskConfigService {

    private static final Logger logger = LoggerFactory.getLogger(MarketCalibrationTaskConfigServiceImpl.class);

    @Autowired
    private TradingPairService tradingPairService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskConfigVO createTaskConfig(TaskConfigCreateRequest request) {
        logger.info("创建任务配置: taskName={}, taskType={}, tradingPairId={}", 
                request.getTaskName(), request.getTaskType(), request.getTradingPairId());

        // 参数校验
        validateTaskType(request.getTaskType());
        validateExecutionMode(request.getExecutionMode(), request.getIntervalHours(), request.getStartTime(), request.getEndTime());

        // 验证交易对是否存在
        TradingPair tradingPair = tradingPairService.getById(request.getTradingPairId());
        if (tradingPair == null) {
            throw new BusinessException("交易对不存在");
        }

        // 创建实体
        MarketCalibrationTaskConfig config = new MarketCalibrationTaskConfig();
        config.setTaskName(request.getTaskName());
        config.setTaskType(request.getTaskType());
        config.setTradingPairId(request.getTradingPairId());
        config.setExecutionMode(request.getExecutionMode());
        config.setIntervalHours(request.getIntervalHours());
        config.setStartTime(request.getStartTime());
        config.setEndTime(request.getEndTime());
        config.setEnabled(request.getEnabled() != null ? request.getEnabled() : EnabledStatus.ENABLED);
        config.setRemark(request.getRemark());

        save(config);

        logger.info("任务配置创建成功: configId={}", config.getId());
        return convertToVO(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskConfigVO updateTaskConfig(Long id, TaskConfigUpdateRequest request) {
        logger.info("更新任务配置: id={}", id);

        MarketCalibrationTaskConfig config = getById(id);
        if (config == null) {
            throw new BusinessException("任务配置不存在");
        }

        // 参数校验
        validateExecutionMode(request.getExecutionMode(), request.getIntervalHours(), request.getStartTime(), request.getEndTime());

        // 更新字段
        config.setTaskName(request.getTaskName());
        config.setExecutionMode(request.getExecutionMode());
        config.setIntervalHours(request.getIntervalHours());
        config.setStartTime(request.getStartTime());
        config.setEndTime(request.getEndTime());
        if (request.getEnabled() != null) {
            config.setEnabled(request.getEnabled());
        }
        config.setRemark(request.getRemark());

        updateById(config);

        logger.info("任务配置更新成功: configId={}", id);
        return convertToVO(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTaskConfig(Long id) {
        logger.info("删除任务配置: id={}", id);

        MarketCalibrationTaskConfig config = getById(id);
        if (config == null) {
            throw new BusinessException("任务配置不存在");
        }

        removeById(id);

        logger.info("任务配置删除成功: configId={}", id);
    }

    @Override
    public TaskConfigVO getTaskConfigById(Long id) {
        logger.debug("根据ID查询任务配置: id={}", id);

        MarketCalibrationTaskConfig config = getById(id);
        if (config == null) {
            throw new BusinessException("任务配置不存在");
        }

        return convertToVO(config);
    }

    @Override
    public IPage<TaskConfigVO> listTaskConfigs(TaskConfigQueryRequest request) {
        logger.debug("查询任务配置列表: taskType={}, tradingPairId={}, executionMode={}, enabled={}", 
                request.getTaskType(), request.getTradingPairId(), request.getExecutionMode(), request.getEnabled());

        // 构建查询条件
        LambdaQueryWrapper<MarketCalibrationTaskConfig> wrapper = new LambdaQueryWrapper<>();
        if (request.getTaskType() != null && !request.getTaskType().isEmpty()) {
            wrapper.eq(MarketCalibrationTaskConfig::getTaskType, request.getTaskType());
        }
        if (request.getTradingPairId() != null) {
            wrapper.eq(MarketCalibrationTaskConfig::getTradingPairId, request.getTradingPairId());
        }
        if (request.getExecutionMode() != null && !request.getExecutionMode().isEmpty()) {
            wrapper.eq(MarketCalibrationTaskConfig::getExecutionMode, request.getExecutionMode());
        }
        if (request.getEnabled() != null) {
            wrapper.eq(MarketCalibrationTaskConfig::getEnabled, request.getEnabled());
        }
        wrapper.orderByDesc(MarketCalibrationTaskConfig::getCreatedAt);

        // 分页查询
        Page<MarketCalibrationTaskConfig> page = new Page<>(request.getCurrent(), request.getSize());
        IPage<MarketCalibrationTaskConfig> configPage = page(page, wrapper);

        // 转换为VO
        Page<TaskConfigVO> voPage = new Page<>(configPage.getCurrent(), configPage.getSize(), configPage.getTotal());
        voPage.setRecords(configPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList()));

        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskConfigVO toggleEnabled(Long id, Boolean enabled) {
        logger.info("启用/禁用任务: id={}, enabled={}", id, enabled);

        MarketCalibrationTaskConfig config = getById(id);
        if (config == null) {
            throw new BusinessException("任务配置不存在");
        }

        config.setEnabled(enabled ? EnabledStatus.ENABLED : EnabledStatus.DISABLED);
        updateById(config);

        logger.info("任务配置状态更新成功: configId={}, enabled={}", id, enabled);
        return convertToVO(config);
    }

    @Override
    public java.util.List<MarketCalibrationTaskConfig> listEnabledAutoTasks(String taskType) {
        logger.debug("查询启用的自动任务配置: taskType={}", taskType);

        LambdaQueryWrapper<MarketCalibrationTaskConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MarketCalibrationTaskConfig::getExecutionMode, ExecutionMode.AUTO);
        wrapper.eq(MarketCalibrationTaskConfig::getEnabled, EnabledStatus.ENABLED);
        if (taskType != null && !taskType.isEmpty()) {
            wrapper.eq(MarketCalibrationTaskConfig::getTaskType, taskType);
        }
        wrapper.orderByAsc(MarketCalibrationTaskConfig::getCreatedAt);

        return list(wrapper);
    }

    /**
     * 校验任务类型
     */
    private void validateTaskType(String taskType) {
        if (!TaskType.MISSING_DATA.equals(taskType) && !TaskType.DATA_VERIFY.equals(taskType)) {
            throw new BusinessException("任务类型无效，必须是 MISSING_DATA 或 DATA_VERIFY");
        }
    }

    /**
     * 校验执行模式
     */
    private void validateExecutionMode(String executionMode, Integer intervalHours, 
                                      java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
        if (!ExecutionMode.AUTO.equals(executionMode) && !ExecutionMode.MANUAL.equals(executionMode)) {
            throw new BusinessException("执行模式无效，必须是 AUTO 或 MANUAL");
        }

        if (ExecutionMode.AUTO.equals(executionMode)) {
            if (intervalHours == null || intervalHours <= 0) {
                throw new BusinessException("自动模式下检测周期必须大于0");
            }
        } else if (ExecutionMode.MANUAL.equals(executionMode)) {
            if (startTime == null || endTime == null) {
                throw new BusinessException("手动模式下开始时间和结束时间不能为空");
            }
            if (!endTime.isAfter(startTime)) {
                throw new BusinessException("结束时间必须晚于开始时间");
            }
        }
    }

    /**
     * 转换为VO
     */
    private TaskConfigVO convertToVO(MarketCalibrationTaskConfig config) {
        TaskConfigVO vo = new TaskConfigVO();
        BeanUtils.copyProperties(config, vo);

        // 获取交易对信息
        TradingPair tradingPair = tradingPairService.getById(config.getTradingPairId());
        if (tradingPair != null) {
            vo.setSymbol(tradingPair.getSymbol());
            vo.setMarketType(tradingPair.getMarketType());
        }

        return vo;
    }
}

