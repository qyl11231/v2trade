package com.qyl.v2trade.business.system.controller;

import com.qyl.v2trade.business.system.model.dto.SignalConfigRequest;
import com.qyl.v2trade.business.system.model.dto.SignalConfigVO;
import com.qyl.v2trade.business.signal.model.entity.SignalConfig;
import com.qyl.v2trade.business.signal.service.SignalConfigService;
import com.qyl.v2trade.common.Result;
import com.qyl.v2trade.common.constants.EnabledStatus;
import com.qyl.v2trade.exception.BusinessException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 信号配置控制器
 */
@RestController
@RequestMapping("/api/signal/config")
public class SignalConfigController {

    private static final Logger logger = LoggerFactory.getLogger(SignalConfigController.class);

    @Autowired
    private SignalConfigService signalConfigService;

    /**
     * 创建信号配置
     */
    @PostMapping
    public Result<SignalConfigVO> createConfig(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody SignalConfigRequest request) {
        logger.info("用户创建信号配置: userId={}, signalName={}, apiKeyId={}", 
                userId, request.getSignalName(), request.getApiKeyId());

        // 参数校验
        if (userId == null) {
            logger.warn("用户ID不能为空");
            return Result.error(400, "用户ID不能为空");
        }

        try {
            SignalConfig config = signalConfigService.createConfig(
                    userId,
                    request.getApiKeyId(),
                    request.getSignalName(),
                    request.getSymbol(),
                    request.getTradingPairId(),
                    request.getEnabled() != null ? request.getEnabled() : EnabledStatus.ENABLED
            );

            return Result.success("创建成功", convertToVO(config));
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("创建信号配置异常", e);
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    /**
     * 更新信号配置
     */
    @PutMapping("/{configId}")
    public Result<SignalConfigVO> updateConfig(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long configId,
            @RequestBody SignalConfigRequest request) {
        logger.info("用户更新信号配置: userId={}, configId={}", userId, configId);

        // 参数校验
        if (userId == null) {
            logger.warn("用户ID不能为空");
            return Result.error(400, "用户ID不能为空");
        }

        try {
            SignalConfig config = signalConfigService.updateConfig(
                    configId,
                    userId,
                    request.getSymbol(),
                    request.getTradingPairId(),
                    request.getEnabled()
            );

            return Result.success("更新成功", convertToVO(config));
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("更新信号配置异常", e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除信号配置
     */
    @DeleteMapping("/{configId}")
    public Result<Void> deleteConfig(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long configId) {
        logger.info("用户删除信号配置: userId={}, configId={}", userId, configId);

        // 参数校验
        if (userId == null) {
            logger.warn("用户ID不能为空");
            return Result.error(400, "用户ID不能为空");
        }

        try {
            signalConfigService.deleteConfig(configId, userId);
            return Result.success("删除成功", null);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("删除信号配置异常", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 查询用户的信号配置列表
     */
    @GetMapping("/list")
    public Result<List<SignalConfigVO>> listConfigs(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(required = false) Long apiKeyId) {
        logger.debug("查询用户信号配置列表: userId={}, apiKeyId={}", userId, apiKeyId);

        // 参数校验
        if (userId == null) {
            logger.warn("用户ID不能为空");
            return Result.error(400, "用户ID不能为空");
        }

        List<SignalConfig> configs;
        if (apiKeyId != null) {
            configs = signalConfigService.listByUserIdAndApiKeyId(userId, apiKeyId);
        } else {
            configs = signalConfigService.listByUserId(userId);
        }

        List<SignalConfigVO> voList = configs.stream()
                .map(this::convertToVO)
                .filter(vo -> vo != null)
                .collect(Collectors.toList());

        return Result.success(voList);
    }

    /**
     * 根据ID查询信号配置详情
     */
    @GetMapping("/{configId}")
    public Result<SignalConfigVO> getConfig(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long configId) {
        logger.debug("查询信号配置详情: userId={}, configId={}", userId, configId);

        // 参数校验
        if (userId == null) {
            logger.warn("用户ID不能为空");
            return Result.error(400, "用户ID不能为空");
        }

        SignalConfig config = signalConfigService.getById(configId);
        if (config == null) {
            return Result.error(404, "信号配置不存在");
        }

        // 权限校验
        if (!config.getUserId().equals(userId)) {
            return Result.error(403, "无权访问该配置");
        }

        return Result.success(convertToVO(config));
    }

    /**
     * 转换为VO
     */
    private SignalConfigVO convertToVO(SignalConfig config) {
        if (config == null) {
            return null;
        }
        SignalConfigVO vo = new SignalConfigVO();
        BeanUtils.copyProperties(config, vo);
        return vo;
    }
}