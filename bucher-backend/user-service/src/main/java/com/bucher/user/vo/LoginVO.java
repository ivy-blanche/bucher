package com.bucher.user.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    /**
     * JWT Token
     */
    private String token;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 学号/工号
     */
    private String userNo;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 角色：1-管理员，2-教师，3-学生
     */
    private Integer role;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 是否需要强制修改密码：0-否，1-是
     */
    private Integer pwdReset;
}
