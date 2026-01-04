package com.qyl.v2trade.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qyl.v2trade.exception.BusinessException;
import com.qyl.v2trade.business.mapper.UserApiKeyMapper;
import com.qyl.v2trade.business.model.dto.ApiKeyRequest;
import com.qyl.v2trade.business.model.dto.ApiKeyVO;
import com.qyl.v2trade.business.model.entity.UserApiKey;
import com.qyl.v2trade.business.service.UserApiKeyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户API Key服务实现类
 */
@Service
public class UserApiKeyServiceImpl extends ServiceImpl<UserApiKeyMapper, UserApiKey> implements UserApiKeyService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiKeyVO addApiKey(Long userId, ApiKeyRequest request) {
        UserApiKey apiKey = new UserApiKey();
        apiKey.setUserId(userId);
        apiKey.setExchange(request.getExchange());
        apiKey.setApiKeyName(request.getApiKeyName());
        apiKey.setApiKey(request.getApiKey());
        apiKey.setSecretKey(request.getSecretKey());
        apiKey.setPassphrase(request.getPassphrase());
        apiKey.setRemark(request.getRemark());
        apiKey.setStatus(1);

        save(apiKey);

        return convertToVO(apiKey);
    }

    @Override
    public List<ApiKeyVO> listByUserId(Long userId) {
        List<UserApiKey> list = list(new LambdaQueryWrapper<UserApiKey>()
                .eq(UserApiKey::getUserId, userId)
                .orderByDesc(UserApiKey::getCreatedAt));

        return list.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public ApiKeyVO getByKeyId(Long userId, Long keyId) {
        UserApiKey apiKey = getAndValidate(userId, keyId);
        return convertToVOFull(apiKey);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiKeyVO updateApiKey(Long userId, Long keyId, ApiKeyRequest request) {
        UserApiKey apiKey = getAndValidate(userId, keyId);

        apiKey.setExchange(request.getExchange());
        apiKey.setApiKeyName(request.getApiKeyName());
        apiKey.setApiKey(request.getApiKey());
        apiKey.setSecretKey(request.getSecretKey());
        apiKey.setPassphrase(request.getPassphrase());
        apiKey.setRemark(request.getRemark());

        updateById(apiKey);

        return convertToVO(apiKey);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteApiKey(Long userId, Long keyId) {
        UserApiKey apiKey = getAndValidate(userId, keyId);
        removeById(apiKey.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiKeyVO toggleStatus(Long userId, Long keyId) {
        UserApiKey apiKey = getAndValidate(userId, keyId);
        
        // 切换状态
        apiKey.setStatus(apiKey.getStatus() == 1 ? 0 : 1);
        updateById(apiKey);

        return convertToVO(apiKey);
    }

    @Override
    public UserApiKey getEnabledApiKey(Long userId, String exchange) {
        return getOne(new LambdaQueryWrapper<UserApiKey>()
                .eq(UserApiKey::getUserId, userId)
                .eq(UserApiKey::getExchange, exchange)
                .eq(UserApiKey::getStatus, 1)
                .last("LIMIT 1"));
    }

    /**
     * 获取并验证API Key所有权
     */
    private UserApiKey getAndValidate(Long userId, Long keyId) {
        UserApiKey apiKey = getById(keyId);
        if (apiKey == null) {
            throw new BusinessException("API Key不存在");
        }
        if (!apiKey.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此API Key");
        }
        return apiKey;
    }

    /**
     * 转换为VO（脱敏处理）
     */
    private ApiKeyVO convertToVO(UserApiKey apiKey) {
        ApiKeyVO vo = new ApiKeyVO();
        vo.setId(apiKey.getId());
        vo.setExchange(apiKey.getExchange());
        vo.setApiKeyName(apiKey.getApiKeyName());
        // API Key脱敏：显示前4位和后4位
        vo.setApiKey(maskString(apiKey.getApiKey()));
        // Secret Key完全隐藏
        vo.setSecretKey("********");
        vo.setStatus(apiKey.getStatus());
        vo.setRemark(apiKey.getRemark());
        vo.setCreatedAt(apiKey.getCreatedAt());
        vo.setUpdatedAt(apiKey.getUpdatedAt());
        return vo;
    }

    /**
     * 转换为完整VO（不脱敏，用于编辑）
     */
    private ApiKeyVO convertToVOFull(UserApiKey apiKey) {
        ApiKeyVO vo = new ApiKeyVO();
        vo.setId(apiKey.getId());
        vo.setExchange(apiKey.getExchange());
        vo.setApiKeyName(apiKey.getApiKeyName());
        vo.setApiKey(apiKey.getApiKey());
        vo.setSecretKey(apiKey.getSecretKey());
        vo.setPassphrase(apiKey.getPassphrase());
        vo.setStatus(apiKey.getStatus());
        vo.setRemark(apiKey.getRemark());
        vo.setCreatedAt(apiKey.getCreatedAt());
        vo.setUpdatedAt(apiKey.getUpdatedAt());
        return vo;
    }

    /**
     * 字符串脱敏
     */
    private String maskString(String str) {
        if (str == null || str.length() <= 8) {
            return "****";
        }
        return str.substring(0, 4) + "****" + str.substring(str.length() - 4);
    }
}
