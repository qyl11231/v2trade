package com.qyl.v2trade.business.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qyl.v2trade.common.Result;
import com.qyl.v2trade.business.model.dto.RegisterRequest;
import com.qyl.v2trade.business.model.dto.UserInfoVO;
import com.qyl.v2trade.business.model.entity.SysUser;
import com.qyl.v2trade.business.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理员用户管理控制器
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    @Autowired
    private UserService userService;

    /**
     * 分页查询用户列表
     */
    @GetMapping
    public Result<Page<UserInfoVO>> listUsers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(required = false) String username) {
        
        Page<SysUser> userPage = new Page<>(page, limit);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        
        if (username != null && !username.trim().isEmpty()) {
            wrapper.like(SysUser::getUsername, username);
        }
        
        userService.page(userPage, wrapper);
        
        Page<UserInfoVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        List<UserInfoVO> voList = userPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        
        return Result.success(voPage);
    }

    /**
     * 添加用户（管理员）
     */
    @PostMapping
    public Result<UserInfoVO> addUser(@Valid @RequestBody RegisterRequest request) {
        UserInfoVO userInfo = userService.register(request);
        return Result.success("用户添加成功", userInfo);
    }

    /**
     * 启用/禁用用户
     */
    @PostMapping("/{userId}/toggle")
    public Result<Void> toggleUserStatus(@PathVariable Long userId) {
        SysUser user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        user.setEnabled(user.getEnabled() == 1 ? 0 : 1);
        userService.updateById(user);
        
        return Result.success(user.getEnabled() == 1 ? "用户已启用" : "用户已禁用", null);
    }

    /**
     * 删除用户（逻辑删除）
     */
    @DeleteMapping("/{userId}")
    public Result<Void> deleteUser(@PathVariable Long userId) {
        userService.removeById(userId);
        return Result.success("用户已删除", null);
    }

    /**
     * 重置用户密码
     */
    @PostMapping("/{userId}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long userId) {
        SysUser user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        // 重置为默认密码：123456
        user.setPassword(org.springframework.util.DigestUtils.md5DigestAsHex("123456".getBytes()));
        userService.updateById(user);
        
        return Result.success("密码已重置为：123456", null);
    }

    private UserInfoVO convertToVO(SysUser user) {
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setEnabled(user.getEnabled());
        vo.setLastLoginAt(user.getLastLoginAt());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}