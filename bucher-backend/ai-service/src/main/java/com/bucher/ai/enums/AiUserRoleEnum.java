package com.bucher.ai.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户角色枚举（与 user-service 的 UserRoleEnum 保持一致，避免跨模块依赖）
 */
@Getter
@AllArgsConstructor
public enum AiUserRoleEnum {

    ADMIN(1, "管理员"),
    TEACHER(2, "教师"),
    STUDENT(3, "学生");

    private final Integer code;
    private final String desc;

    public static AiUserRoleEnum getByCode(Integer code) {
        for (AiUserRoleEnum role : values()) {
            if (role.getCode().equals(code)) {
                return role;
            }
        }
        return null;
    }
}
