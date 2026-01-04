package com.qyl.v2trade.business.controller;

import com.qyl.v2trade.common.Result;
import com.qyl.v2trade.business.model.dto.ApiKeyRequest;
import com.qyl.v2trade.business.model.dto.ApiKeyVO;
import com.qyl.v2trade.business.service.UserApiKeyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户API Key控制器
 */
@RestController
@RequestMapping("/api/user/api-keys")
public class UserApiKeyController {

    @Autowired
    private UserApiKeyService userApiKeyService;

    /**
     * 获取用户的所有API Key
     */
    @GetMapping
    public Result<List<ApiKeyVO>> listApiKeys(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        // 参数校验
        if (userId == null) {
            return Result.error(400, "用户ID不能为空");
        }
        
        List<ApiKeyVO> list = userApiKeyService.listByUserId(userId);
        return Result.success(list);
    }

    /**
     * 获取单个API Key详情
     */
    @GetMapping("/{keyId}")
    public Result<ApiKeyVO> getApiKey(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long keyId) {
        // 参数校验
        if (userId == null) {
            return Result.error(400, "用户ID不能为空");
        }
        
        ApiKeyVO apiKey = userApiKeyService.getByKeyId(userId, keyId);
        return Result.success(apiKey);
    }

    /**
     * 添加API Key
     */
    @PostMapping
    public Result<ApiKeyVO> addApiKey(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody ApiKeyRequest request) {
        // 参数校验
        if (userId == null) {
            return Result.error(400, "用户ID不能为空");
        }
        
        ApiKeyVO apiKey = userApiKeyService.addApiKey(userId, request);
        return Result.success("添加成功", apiKey);
    }

    /**
     * 更新API Key
     */
    @PutMapping("/{keyId}")
    public Result<ApiKeyVO> updateApiKey(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long keyId,
            @Valid @RequestBody ApiKeyRequest request) {
        // 参数校验
        if (userId == null) {
            return Result.error(400, "用户ID不能为空");
        }
        
        ApiKeyVO apiKey = userApiKeyService.updateApiKey(userId, keyId, request);
        return Result.success("更新成功", apiKey);
    }

    /**
     * 删除API Key
     */
    @DeleteMapping("/{keyId}")
    public Result<Void> deleteApiKey(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long keyId) {
        // 参数校验
        if (userId == null) {
            return Result.error(400, "用户ID不能为空");
        }
        
        userApiKeyService.deleteApiKey(userId, keyId);
        return Result.success("删除成功", null);
    }

    /**
     * 切换API Key状态
     */
    @PostMapping("/{keyId}/toggle")
    public Result<ApiKeyVO> toggleStatus(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long keyId) {
        // 参数校验
        if (userId == null) {
            return Result.error(400, "用户ID不能为空");
        }
        
        ApiKeyVO apiKey = userApiKeyService.toggleStatus(userId, keyId);
        return Result.success("状态更新成功", apiKey);
    }
}