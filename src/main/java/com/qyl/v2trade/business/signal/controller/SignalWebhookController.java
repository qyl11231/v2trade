package com.qyl.v2trade.business.signal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qyl.v2trade.business.signal.model.entity.Signal;
import com.qyl.v2trade.business.system.model.webhook.TradingViewWebhookRequest;
import com.qyl.v2trade.business.signal.service.SignalService;
import com.qyl.v2trade.common.Result;
import com.qyl.v2trade.common.constants.SignalSource;
import com.qyl.v2trade.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 信号Webhook控制器
 * 用于接收TradingView等外部系统的信号
 */
@RestController
@RequestMapping("/api/signal/webhook")
public class SignalWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(SignalWebhookController.class);

    @Autowired
    private SignalService signalService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * TradingView Webhook 信号接入端点
     * 支持 application/json 和 text/plain 格式
     * 
     * 请求体格式：
     * {
     *   "signal_config_id": 1,
     *   "signal_name": "kong-wu",
     *   "symbol": "{{ticker}}",
     *   "action": "{{strategy.order.action}}",
     *   "price": {{strategy.order.price}},
     *   "quantity": {{strategy.order.contracts}},
     *   "timeframe": "{{interval}}",
     *   "signalId": "{{strategy.order.id}}",
     *   "timestamp": "{{timenow}}"
     * }
     * 
     * @param payload TradingView信号请求（JSON字符串）
     * @return 处理结果
     */
    @PostMapping(value = "/tradingview", consumes = {"application/json", "text/plain"})
    public Result<Signal> receiveTradingViewSignal(@RequestBody String payload) {
        logger.info("收到TradingView信号: {}", payload);
        
        try {
            // 解析请求体获取signal_config_id
            TradingViewWebhookRequest request = objectMapper.readValue(payload, TradingViewWebhookRequest.class);
            
            // 校验必填字段
            if (request.getSignalConfigId() == null) {
                logger.warn("缺少必填字段 signal_config_id");
                throw new BusinessException(400, "缺少必填字段 signal_config_id");
            }
            
            logger.info("解析TradingView信号成功: signalConfigId={}, signalName={}, symbol={}, action={}", 
                    request.getSignalConfigId(), request.getSignalName(), request.getSymbol(), request.getAction());
            
            // 调用信号服务处理
            Signal signal = signalService.ingestSignal(
                    request.getSignalConfigId(), 
                    SignalSource.TRADING_VIEW, 
                    payload
            );
            
            return Result.success(signal);
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("处理TradingView信号失败: {}", e.getMessage(), e);
            throw new BusinessException(400, "信号格式解析失败: " + e.getMessage());
        }
    }
}