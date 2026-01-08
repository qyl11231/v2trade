package com.qyl.v2trade.business.signal.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qyl.v2trade.business.signal.model.entity.Signal;

import java.util.List;

/**
 * 信号服务接口
 */
public interface SignalService extends IService<Signal> {

    /**
     * 校验信号并入库
     * @param singalConfigId 信号配置ID ID
     * @param signalSource 信号来源
     * @param rawPayload 原始信号内容（JSON字符串）
     * @return 入库的信号实体，校验失败抛出异常
     */
    public Signal ingestSignal( Long singalConfigId, String signalSource, String rawPayload) ;

    /**
     * 根据用户ID查询信号列表
     * @param userId 用户ID
     * @param limit 查询数量限制
     * @return 信号列表
     */
    List<Signal> listByUserId(Long userId, Integer limit);

    /**
     * 根据用户ID、API Key ID和交易对查询信号列表
     * @param userId 用户ID
     * @param apiKeyId API Key ID
     * @param symbol 交易对
     * @param limit 查询数量限制
     * @return 信号列表
     */
    List<Signal> listByUserIdAndApiKeyIdAndSymbol(Long userId, Long apiKeyId, String symbol, Integer limit);
}

