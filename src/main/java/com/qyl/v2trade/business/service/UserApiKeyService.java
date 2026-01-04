package com.qyl.v2trade.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qyl.v2trade.business.model.dto.ApiKeyRequest;
import com.qyl.v2trade.business.model.dto.ApiKeyVO;
import com.qyl.v2trade.business.model.entity.UserApiKey;

import java.util.List;

/**
 * 用户API Key服务接口
 */
public interface UserApiKeyService extends IService<UserApiKey> {

    /**
     * 添加API Key
     * @param userId 用户ID
     * @param request API Key请求
     * @return API Key信息
     */
    ApiKeyVO addApiKey(Long userId, ApiKeyRequest request);

    /**
     * 获取用户的所有API Key
     * @param userId 用户ID
     * @return API Key列表
     */
    List<ApiKeyVO> listByUserId(Long userId);

    /**
     * 根据ID获取API Key详情
     * @param userId 用户ID
     * @param keyId API Key ID
     * @return API Key信息
     */
    ApiKeyVO getByKeyId(Long userId, Long keyId);

    /**
     * 更新API Key
     * @param userId 用户ID
     * @param keyId API Key ID
     * @param request API Key请求
     * @return 更新后的API Key信息
     */
    ApiKeyVO updateApiKey(Long userId, Long keyId, ApiKeyRequest request);

    /**
     * 删除API Key
     * @param userId 用户ID
     * @param keyId API Key ID
     */
    void deleteApiKey(Long userId, Long keyId);

    /**
     * 切换API Key状态
     * @param userId 用户ID
     * @param keyId API Key ID
     * @return 更新后的API Key信息
     */
    ApiKeyVO toggleStatus(Long userId, Long keyId);

    /**
     * 获取用户启用的API Key
     * @param userId 用户ID
     * @param exchange 交易所
     * @return API Key实体
     */
    UserApiKey getEnabledApiKey(Long userId, String exchange);
}
