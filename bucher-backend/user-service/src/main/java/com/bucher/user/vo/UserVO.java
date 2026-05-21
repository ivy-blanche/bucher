package com.bucher.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息 VO
 */
@Data
public class UserVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 学号/工号
     */
    private String userNo;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 角色：1-管理员，2-教师，3-学生
     */
    private Integer role;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 所属院系ID
     */
    private Long deptId;

    /**
     * 所属院系名称
     */
    private String deptName;

    /**
     * 所属行政班级ID
     */
    private Long adminClassId;

    /**
     * 所属行政班级名称
     */
    private String adminClassName;

    /**
     * 用户来源：1-管理员添加，2-自主注册
     */
    private Integer source;

    /**
     * 教师申请审核状态：0-待审核，1-通过，2-拒绝，NULL-未申请
     */
    private Integer auditStatus;

    /**
     * 状态：1-正常，0-禁用
     */
    private Integer status;

    /**
     * 密码是否已被管理员重置
     */
    private Integer pwdReset;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
