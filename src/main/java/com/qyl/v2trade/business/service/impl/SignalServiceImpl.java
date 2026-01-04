package com.qyl.v2trade.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qyl.v2trade.business.mapper.SignalMapper;
import com.qyl.v2trade.business.model.entity.Signal;
import com.qyl.v2trade.business.model.entity.SignalConfig;
import com.qyl.v2trade.business.service.SignalConfigService;
import com.qyl.v2trade.business.service.SignalService;
import com.qyl.v2trade.common.constants.EnabledStatus;
import com.qyl.v2trade.common.constants.SignalDirectionHint;
import com.qyl.v2trade.common.constants.TradingViewAction;
import com.qyl.v2trade.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 信号服务实现类
 */
@Service
public class SignalServiceImpl extends ServiceImpl<SignalMapper, Signal> implements SignalService {

    private static final Logger logger = LoggerFactory.getLogger(SignalServiceImpl.class);

    @Autowired
    private SignalConfigService signalConfigService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Signal ingestSignal( Long singalConfigId, String signalSource, String rawPayload) {
        logger.info("开始处理信号接入:singalConfigId={}, signalSource={}", singalConfigId, signalSource);

        // 1. 根据 signalName 和 apiKeyId 查询 signal_config（白名单校验）
        SignalConfig config = signalConfigService.getConfigById(singalConfigId);
        if (config == null || !config.getEnabled().equals(EnabledStatus.ENABLED)) {
            logger.warn("信号配置不存在或未启用，拒绝信号: singalConfigId={}={}", singalConfigId);
            throw new BusinessException(400, "信号配置不存在或未启用");
        }

        logger.info("信号配置校验通过: configId={}, userId={}, symbol={}", 
                config.getId(), config.getUserId(), config.getSymbol());

        // 2. 解析原始信号内容
        Signal signal = parseSignalFromPayload(config, signalSource, rawPayload);
        
        // 3. 写入 signal 表
        save(signal);
        
        logger.info("信号入库成功: signalId={}, signalName={}, symbol={}", 
                signal.getId(), signal.getSignalName(), signal.getSymbol());
        
        return signal;
    }

    @Override
    public List<Signal> listByUserId(Long userId, Integer limit) {
        logger.debug("查询用户信号列表: userId={}, limit={}", userId, limit);
        
        LambdaQueryWrapper<Signal> wrapper = new LambdaQueryWrapper<Signal>()
                .eq(Signal::getUserId, userId)
                .orderByDesc(Signal::getReceivedAt)
                .orderByDesc(Signal::getCreatedAt);
        
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        
        return list(wrapper);
    }

    @Override
    public List<Signal> listByUserIdAndApiKeyIdAndSymbol(Long userId, Long apiKeyId, String symbol, Integer limit) {
        logger.debug("查询信号列表: userId={}, apiKeyId={}, symbol={}, limit={}", userId, apiKeyId, symbol, limit);
        
        LambdaQueryWrapper<Signal> wrapper = new LambdaQueryWrapper<Signal>()
                .eq(Signal::getUserId, userId)
                .eq(Signal::getApiKeyId, apiKeyId)
                .eq(Signal::getSymbol, symbol)
                .orderByDesc(Signal::getReceivedAt)
                .orderByDesc(Signal::getCreatedAt);
        
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        
        return list(wrapper);
    }

    /**
     * 从原始payload解析信号数据
     */
    private Signal parseSignalFromPayload(SignalConfig config, String signalSource, String rawPayload) {
        Signal signal = new Signal();
        signal.setUserId(config.getUserId());
        signal.setApiKeyId(config.getApiKeyId());
        signal.setSignalConfigId(config.getId());
        signal.setSignalSource(signalSource);
        signal.setSignalName(config.getSignalName());
        signal.setSymbol(config.getSymbol());
        signal.setRawPayload(rawPayload);
        signal.setReceivedAt(LocalDateTime.now());

        try {
            // 解析JSON payload，提取可选的信号信息
            JsonNode jsonNode = objectMapper.readTree(rawPayload);
            
            // 提取方向提示（action字段，如 "buy", "sell", "close"）
            if (jsonNode.has("action")) {
                String action = jsonNode.get("action").asText().toUpperCase();
                // 映射到信号方向提示
                if (action.contains(TradingViewAction.BUY) || action.contains(TradingViewAction.LONG)) {
                    signal.setSignalDirectionHint(SignalDirectionHint.LONG);
                } else if (action.contains(TradingViewAction.SELL) || action.contains(TradingViewAction.SHORT)) {
                    signal.setSignalDirectionHint(SignalDirectionHint.SHORT);
                } else {
                    signal.setSignalDirectionHint(SignalDirectionHint.NEUTRAL);
                }
            } else {
                // 默认值
                signal.setSignalDirectionHint(SignalDirectionHint.NEUTRAL);
            }

            // 提取价格（price字段）
            if (jsonNode.has("price")) {
                signal.setPrice(new BigDecimal(jsonNode.get("price").asText()));
            }

            // 提取数量（quantity或qty字段）
            if (jsonNode.has("quantity")) {
                signal.setQuantity(new BigDecimal(jsonNode.get("quantity").asText()));
            } else if (jsonNode.has("qty")) {
                signal.setQuantity(new BigDecimal(jsonNode.get("qty").asText()));
            }

            // 提取事件类型（event_type或type字段）
            if (jsonNode.has("event_type")) {
                signal.setSignalEventType(jsonNode.get("event_type").asText().toUpperCase());
            }

        } catch (Exception e) {
            logger.warn("解析信号payload失败，使用默认值: {}", e.getMessage());
            // 解析失败时使用默认值，但不影响入库
            signal.setSignalDirectionHint(SignalDirectionHint.NEUTRAL);
        }

        return signal;
    }
}
