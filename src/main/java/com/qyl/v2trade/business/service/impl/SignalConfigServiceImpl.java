package com.qyl.v2trade.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qyl.v2trade.business.mapper.SignalConfigMapper;
import com.qyl.v2trade.business.model.entity.SignalConfig;
import com.qyl.v2trade.business.service.SignalConfigService;
import com.qyl.v2trade.common.constants.EnabledStatus;
import com.qyl.v2trade.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 信号配置服务实现类
 */
@Service
public class SignalConfigServiceImpl extends ServiceImpl<SignalConfigMapper, SignalConfig> implements SignalConfigService {

    private static final Logger logger = LoggerFactory.getLogger(SignalConfigServiceImpl.class);

    @Override
    public SignalConfig getConfigById(Long id) {
        logger.debug("根据ID查询信号配置: id={}", id);
        return getById(id);
    }

    @Override
    public SignalConfig getEnabledConfig(String signalName, Long apiKeyId) {
        logger.debug("查询信号配置: signalName={}, apiKeyId={}", signalName, apiKeyId);
        
        return getOne(new LambdaQueryWrapper<SignalConfig>()
                .eq(SignalConfig::getSignalName, signalName)
                .eq(SignalConfig::getApiKeyId, apiKeyId)
                .eq(SignalConfig::getEnabled, EnabledStatus.ENABLED));
    }

    @Override
    public List<SignalConfig> listByUserId(Long userId) {
        logger.debug("查询用户信号配置列表: userId={}", userId);
        
        return list(new LambdaQueryWrapper<SignalConfig>()
                .eq(SignalConfig::getUserId, userId)
                .orderByDesc(SignalConfig::getCreatedAt));
    }

    @Override
    public List<SignalConfig> listEnabledConfigsByUserId(Long userId) {
        logger.debug("查询用户启用的信号配置列表: userId={}", userId);
        
        return list(new LambdaQueryWrapper<SignalConfig>()
                .eq(SignalConfig::getUserId, userId)
                .eq(SignalConfig::getEnabled, EnabledStatus.ENABLED)
                .orderByDesc(SignalConfig::getCreatedAt));
    }

    @Override
    public List<SignalConfig> listEnabledConfigsByUserIdAndApiKeyId(Long userId, Long apiKeyId) {
        logger.debug("查询用户和API Key启用的信号配置列表: userId={}, apiKeyId={}", userId, apiKeyId);
        
        return list(new LambdaQueryWrapper<SignalConfig>()
                .eq(SignalConfig::getUserId, userId)
                .eq(SignalConfig::getApiKeyId, apiKeyId)
                .eq(SignalConfig::getEnabled, 1)
                .orderByDesc(SignalConfig::getCreatedAt));
    }

    @Override
    public List<SignalConfig> listByUserIdAndApiKeyId(Long userId, Long apiKeyId) {
        logger.debug("查询用户和API Key的信号配置列表: userId={}, apiKeyId={}", userId, apiKeyId);
        
        return list(new LambdaQueryWrapper<SignalConfig>()
                .eq(SignalConfig::getUserId, userId)
                .eq(SignalConfig::getApiKeyId, apiKeyId)
                .orderByDesc(SignalConfig::getCreatedAt));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SignalConfig createConfig(Long userId, Long apiKeyId, String signalName, String symbol, Long tradingPairId, Integer enabled) {
        logger.info("创建信号配置: userId={}, apiKeyId={}, signalName={}, symbol={}, tradingPairId={}", 
                userId, apiKeyId, signalName, symbol, tradingPairId);

        // 检查是否已存在相同的信号配置（signal_name + api_key_id 唯一约束）
        SignalConfig existConfig = getOne(new LambdaQueryWrapper<SignalConfig>()
                .eq(SignalConfig::getSignalName, signalName)
                .eq(SignalConfig::getApiKeyId, apiKeyId));
        
        if (existConfig != null) {
            throw new BusinessException(400, "该信号名称在此API Key下已存在");
        }

        SignalConfig config = new SignalConfig();
        config.setUserId(userId);
        config.setApiKeyId(apiKeyId);
        config.setSignalName(signalName.trim());
        config.setSymbol(symbol.trim());
        config.setTradingPairId(tradingPairId);
        config.setEnabled(enabled != null && enabled.equals(EnabledStatus.ENABLED) ? EnabledStatus.ENABLED : EnabledStatus.DISABLED);

        save(config);
        
        logger.info("信号配置创建成功: configId={}", config.getId());
        return config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SignalConfig updateConfig(Long configId, Long userId, String symbol, Long tradingPairId, Integer enabled) {
        logger.info("更新信号配置: configId={}, userId={}", configId, userId);

        SignalConfig config = getById(configId);
        if (config == null) {
            throw new BusinessException(404, "信号配置不存在");
        }

        // 权限校验：只能更新自己的配置
        if (!config.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该配置");
        }

        if (symbol != null && !symbol.trim().isEmpty()) {
            config.setSymbol(symbol.trim());
        }
        if (tradingPairId != null) {
            config.setTradingPairId(tradingPairId);
        }
        if (enabled != null) {
            config.setEnabled(enabled.equals(EnabledStatus.ENABLED) ? EnabledStatus.ENABLED : EnabledStatus.DISABLED);
        }

        updateById(config);
        
        logger.info("信号配置更新成功: configId={}", configId);
        return config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(Long configId, Long userId) {
        logger.info("删除信号配置: configId={}, userId={}", configId, userId);

        SignalConfig config = getById(configId);
        if (config == null) {
            throw new BusinessException(404, "信号配置不存在");
        }

        // 权限校验：只能删除自己的配置
        if (!config.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该配置");
        }

        // 物理删除（因为signal表有外键关联，需要考虑级联删除或先检查是否有关联信号）
        // 这里先简单实现物理删除，生产环境需要根据业务需求调整
        removeById(configId);
        
        logger.info("信号配置删除成功: configId={}", configId);
    }
}
