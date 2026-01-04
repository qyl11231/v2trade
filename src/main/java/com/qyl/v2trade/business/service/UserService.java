package com.qyl.v2trade.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qyl.v2trade.business.model.dto.LoginRequest;
import com.qyl.v2trade.business.model.dto.RegisterRequest;
import com.qyl.v2trade.business.model.dto.UserInfoVO;
import com.qyl.v2trade.business.model.entity.SysUser;

/**
 * 用户服务接口
 */
public interface UserService extends IService<SysUser> {

    /**
     * 用户登录
     * @param request 登录请求
     * @return 用户信息
     */
    UserInfoVO login(LoginRequest request);

    /**
     * 用户注册
     * @param request 注册请求
     * @return 用户信息
     */
    UserInfoVO register(RegisterRequest request);

    /**
     * 根据用户名获取用户
     * @param username 用户名
     * @return 用户实体
     */
    SysUser getByUsername(String username);

    /**
     * 修改密码
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    void changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 更新用户信息
     * @param userId 用户ID
     * @param nickname 昵称
     * @param email 邮箱
     * @return 更新后的用户信息
     */
    UserInfoVO updateUserInfo(Long userId, String nickname, String email);
}
