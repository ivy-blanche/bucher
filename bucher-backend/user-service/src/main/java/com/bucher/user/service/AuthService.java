package com.bucher.user.service;

import com.bucher.user.dto.*;
import com.bucher.user.vo.LoginVO;
import com.bucher.user.vo.UserVO;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 登录
     */
    LoginVO login(LoginDTO dto);

    /**
     * 注册
     */
    void register(RegisterDTO dto);

    /**
     * 发送验证码
     */
    void sendCode(SendCodeDTO dto);

    /**
     * 重置密码
     */
    void resetPassword(ResetPasswordDTO dto);

    /**
     * 登出
     */
    void logout(Long userId);

    /**
     * 获取当前用户信息
     */
    UserVO getCurrentUser(Long userId);

    /**
     * 强制修改密码（密码被管理员重置后）
     */
    void changePassword(Long userId, ChangePasswordDTO dto);
}
