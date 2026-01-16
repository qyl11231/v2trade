package com.qyl.v2trade.business.signal.controller;

import com.qyl.v2trade.business.system.model.dto.SignalVO;
import com.qyl.v2trade.business.signal.model.entity.Signal;
import com.qyl.v2trade.business.signal.service.SignalService;
import com.qyl.v2trade.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 信号查询控制器
 * 供策略模块、统计模块等消费使用
 */
@RestController
@RequestMapping("/api/signal/query")
public class SignalQueryController {

    private static final Logger logger = LoggerFactory.getLogger(SignalQueryController.class);

    @Autowired
    private SignalService signalService;

    /**
     * 查询用户的信号列表
     * 
     * @param userId 用户ID（从请求头获取）
     * @param limit 查询数量限制（可选，默认100）
     * @return 信号列表
     */
    @GetMapping("/list")
    public Result<List<SignalVO>> listSignals(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(required = false, defaultValue = "100") Integer limit) {
        logger.debug("查询用户信号列表: userId={}, limit={}", userId, limit);
        
        // 参数校验
        if (userId == null) {
            logger.warn("用户ID不能为空");
            return Result.error(400, "用户ID不能为空");
        }

        List<Signal> signals = signalService.listByUserId(userId, limit);
        List<SignalVO> voList = signals.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return Result.success(voList);
    }

    /**
     * 根据用户ID、API Key ID和交易对查询信号列表
     * 
     * @param userId 用户ID（从请求头获取）
     * @param apiKeyId API Key ID（查询参数）
     * @param symbol 交易对（查询参数）
     * @param limit 查询数量限制（可选，默认100）
     * @return 信号列表
     */
    @GetMapping("/list/by-params")
    public Result<List<SignalVO>> listSignalsByParams(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam Long apiKeyId,
            @RequestParam String symbol,
            @RequestParam(required = false, defaultValue = "100") Integer limit) {
        logger.debug("查询信号列表: userId={}, apiKeyId={}, symbol={}, limit={}", userId, apiKeyId, symbol, limit);
        
        // 参数校验
        if (userId == null) {
            logger.warn("用户ID不能为空");
            return Result.error(400, "用户ID不能为空");
        }

        List<Signal> signals = signalService.listByUserIdAndApiKeyIdAndSymbol(userId, apiKeyId, symbol, limit);
        List<SignalVO> voList = signals.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return Result.success(voList);
    }

    /**
     * 转换为VO
     */
    private SignalVO convertToVO(Signal signal) {
        if (signal == null) {
            return null;
        }
        SignalVO vo = new SignalVO();
        BeanUtils.copyProperties(signal, vo);
        return vo;
    }
}