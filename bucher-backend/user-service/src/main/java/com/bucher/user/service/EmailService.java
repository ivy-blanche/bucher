package com.bucher.user.service;

/**
 * 邮件服务接口
 */
public interface EmailService {

    /**
     * 发送验证码
     *
     * @param email 邮箱
     * @param type  类型：register-注册，reset-重置密码
     */
    void sendVerificationCode(String email, String type);

    /**
     * 验证验证码
     *
     * @param email 邮箱
     * @param code  验证码
     * @return 是否验证成功
     */
    boolean verifyCode(String email, String code);

    /**
     * 删除验证码
     *
     * @param email 邮箱
     */
    void deleteCode(String email);
}
