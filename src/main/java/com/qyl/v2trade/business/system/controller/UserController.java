package com.qyl.v2trade.business.system.controller;

import com.qyl.v2trade.business.system.model.dto.*;
import com.qyl.v2trade.common.Result;
import com.qyl.v2trade.business.system.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<UserInfoVO> login(@Valid @RequestBody LoginRequest request) {
        UserInfoVO userInfo = userService.login(request);
        return Result.success("登录成功", userInfo);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<UserInfoVO> register(@Valid @RequestBody RegisterRequest request) {
        UserInfoVO userInfo = userService.register(request);
        return Result.success("注册成功", userInfo);
    }

    /**
     * 获取当前用户信息
     * 注意：这里暂时从请求头获取userId，后续需要接入JWT认证
     */
    @GetMapping("/info")
    public Result<UserInfoVO> getUserInfo(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        // 参数校验
        if (userId == null) {
            return Result.error(400, "用户ID不能为空");
        }
        
        var user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setEnabled(user.getEnabled());
        vo.setLastLoginAt(user.getLastLoginAt());
        vo.setCreatedAt(user.getCreatedAt());
        return Result.success(vo);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/info")
    public Result<UserInfoVO> updateUserInfo(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody UpdateUserRequest request) {
        // 参数校验
        if (userId == null) {
            return Result.error(400, "用户ID不能为空");
        }
        
        UserInfoVO userInfo = userService.updateUserInfo(userId, request.getNickname(), request.getEmail());
        return Result.success("更新成功", userInfo);
    }

    /**
     * 修改密码
     */
    @PostMapping("/password")
    public Result<Void> changePassword(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        // 参数校验
        if (userId == null) {
            return Result.error(400, "用户ID不能为空");
        }
        
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return Result.error("两次密码输入不一致");
        }
        userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return Result.success("密码修改成功", null);
    }
}