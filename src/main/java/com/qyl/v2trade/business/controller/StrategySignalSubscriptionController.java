package com.qyl.v2trade.business.controller;

import com.qyl.v2trade.business.model.dto.StrategySignalSubscriptionRequest;
import com.qyl.v2trade.business.model.dto.StrategySignalSubscriptionVO;
import com.qyl.v2trade.business.model.entity.SignalConfig;
import com.qyl.v2trade.business.model.entity.StrategySignalSubscription;
import com.qyl.v2trade.business.service.SignalConfigService;
import com.qyl.v2trade.business.service.StrategySignalSubscriptionService;
import com.qyl.v2trade.common.Result;
import com.qyl.v2trade.common.constants.ConsumeMode;
import com.qyl.v2trade.common.constants.EnabledStatus;
import com.qyl.v2trade.exception.BusinessException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 策略信号订阅控制器
 */
@RestController
@RequestMapping("/api/strategy/subscription")
public class StrategySignalSubscriptionController {

    private static final Logger logger = LoggerFactory.getLogger(StrategySignalSubscriptionController.class);

    @Autowired
    private StrategySignalSubscriptionService subscriptionService;

    @Autowired
    private SignalConfigService signalConfigService;

    /**
     * 创建策略信号订阅
     */
    @PostMapping
    public Result<StrategySignalSubscriptionVO> createSubscription(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody StrategySignalSubscriptionRequest request) {
        logger.info("用户创建策略信号订阅: userId={}, strategyId={}, signalConfigId={}", 
                userId, request.getStrategyId(), request.getSignalConfigId());

        if (userId == null) {
            logger.warn("用户ID不能为空");
            return Result.error(400, "用户ID不能为空");
        }

        try {
            StrategySignalSubscription subscription = subscriptionService.createSubscription(
                    userId,
                    request.getStrategyId(),
                    request.getSignalConfigId(),
                    request.getConsumeMode() != null ? request.getConsumeMode() : ConsumeMode.LATEST_ONLY,
                    request.getEnabled() != null ? request.getEnabled() : EnabledStatus.ENABLED
            );

            return Result.success("创建成功", convertToVO(subscription));
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("创建策略信号订阅异常", e);
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    /**
     * 更新策略信号订阅
     */
    @PutMapping("/{id}")
    public Result<StrategySignalSubscriptionVO> updateSubscription(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long id,
            @RequestBody StrategySignalSubscriptionRequest request) {
        logger.info("用户更新策略信号订阅: userId={}, id={}", userId, id);

        if (userId == null) {
            logger.warn("用户ID不能为空");
            return Result.error(400, "用户ID不能为空");
        }

        try {
            StrategySignalSubscription subscription = subscriptionService.updateSubscription(
                    id,
                    userId,
                    request.getConsumeMode(),
                    request.getEnabled()
            );

            return Result.success("更新成功", convertToVO(subscription));
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("更新策略信号订阅异常", e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除策略信号订阅
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteSubscription(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long id) {
        logger.info("用户删除策略信号订阅: userId={}, id={}", userId, id);

        if (userId == null) {
            logger.warn("用户ID不能为空");
            return Result.error(400, "用户ID不能为空");
        }

        try {
            subscriptionService.deleteSubscription(id, userId);
            return Result.success("删除成功", null);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("删除策略信号订阅异常", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 根据策略ID查询订阅列表
     */
    @GetMapping("/list")
    public Result<List<StrategySignalSubscriptionVO>> listSubscriptions(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam Long strategyId) {
        logger.debug("查询策略信号订阅列表: userId={}, strategyId={}", userId, strategyId);

        if (userId == null) {
            logger.warn("用户ID不能为空");
            return Result.error(400, "用户ID不能为空");
        }

        List<StrategySignalSubscription> subscriptions = subscriptionService.listByStrategyId(strategyId);

        // 获取所有信号配置ID，批量查询信号配置信息
        List<Long> signalConfigIds = subscriptions.stream()
                .map(StrategySignalSubscription::getSignalConfigId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, SignalConfig> signalConfigMap = signalConfigService.listByIds(signalConfigIds)
                .stream()
                .collect(Collectors.toMap(SignalConfig::getId, config -> config));

        List<StrategySignalSubscriptionVO> voList = subscriptions.stream()
                .map(subscription -> {
                    StrategySignalSubscriptionVO vo = convertToVO(subscription);
                    SignalConfig config = signalConfigMap.get(subscription.getSignalConfigId());
                    if (config != null) {
                        vo.setSignalConfigName(config.getSignalName());
                    }
                    return vo;
                })
                .filter(vo -> vo != null)
                .collect(Collectors.toList());

        return Result.success(voList);
    }

    /**
     * 转换为VO
     */
    private StrategySignalSubscriptionVO convertToVO(StrategySignalSubscription subscription) {
        if (subscription == null) {
            return null;
        }
        StrategySignalSubscriptionVO vo = new StrategySignalSubscriptionVO();
        BeanUtils.copyProperties(subscription, vo);
        return vo;
    }
}

