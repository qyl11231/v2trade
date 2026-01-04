package com.qyl.v2trade.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qyl.v2trade.business.model.entity.SignalConfig;

import java.util.List;

/**
 * 信号配置服务接口
 */
public interface SignalConfigService extends IService<SignalConfig> {

    /**
     * 根据ID查询信号配置
     * @param id 配置ID
     * @return 信号配置，未找到返回null
     */
    SignalConfig getConfigById(Long id);

    /**
     * 根据信号名称和API Key ID查询启用的配置
     * @param signalName 信号名称
     * @param apiKeyId API Key ID
     * @return 信号配置，未找到或未启用返回null
     */
    SignalConfig getEnabledConfig(String signalName, Long apiKeyId);

    /**
     * 根据用户ID查询所有信号配置
     * @param userId 用户ID
     * @return 信号配置列表
     */
    List<SignalConfig> listByUserId(Long userId);

    /**
     * 根据用户ID查询所有启用的信号配置
     * @param userId 用户ID
     * @return 启用的信号配置列表
     */
    List<SignalConfig> listEnabledConfigsByUserId(Long userId);

    /**
     * 根据用户ID和API Key ID查询启用的信号配置
     * @param userId 用户ID
     * @param apiKeyId API Key ID
     * @return 启用的信号配置列表
     */
    List<SignalConfig> listEnabledConfigsByUserIdAndApiKeyId(Long userId, Long apiKeyId);

    /**
     * 根据用户ID和API Key ID查询信号配置
     * @param userId 用户ID
     * @param apiKeyId API Key ID
     * @return 信号配置列表
     */
    List<SignalConfig> listByUserIdAndApiKeyId(Long userId, Long apiKeyId);

    /**
     * 创建信号配置
     * @param userId 用户ID
     * @param apiKeyId API Key ID
     * @param signalName 信号名称
     * @param symbol 交易对
     * @param tradingPairId 关联交易对ID
     * @param enabled 是否启用
     * @return 创建的信号配置
     */
    SignalConfig createConfig(Long userId, Long apiKeyId, String signalName, String symbol, Long tradingPairId, Integer enabled);

    /**
     * 更新信号配置
     * @param configId 配置ID
     * @param userId 用户ID（用于权限校验）
     * @param symbol 交易对（可选）
     * @param tradingPairId 关联交易对ID（可选）
     * @param enabled 是否启用（可选）
     * @return 更新后的信号配置
     */
    SignalConfig updateConfig(Long configId, Long userId, String symbol, Long tradingPairId, Integer enabled);

    /**
     * 删除信号配置（逻辑删除，这里使用物理删除，因为signal表外键关联）
     * @param configId 配置ID
     * @param userId 用户ID（用于权限校验）
     */
    void deleteConfig(Long configId, Long userId);
}
