package com.bucher.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 工号/学号（唯一登录账号）
     */
    private String userNo;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 密码（BCrypt加密）
     */
    private String password;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 头像URL（MinIO路径）
     */
    private String avatarUrl;

    /**
     * 角色：1-管理员，2-教师，3-学生
     */
    private Integer role;

    /**
     * 所属院系/部门ID
     */
    private Long deptId;

    /**
     * 所属行政班级ID（仅学生有值）
     */
    private Long adminClassId;

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
     * 密码是否已被管理员重置：0-否，1-是（需强制修改密码）
     */
    private Integer pwdReset;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    @TableLogic
    private Integer isDeleted;
}
