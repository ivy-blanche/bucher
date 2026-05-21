package com.bucher.user.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户角色枚举
 */
@Getter
@AllArgsConstructor
public enum UserRoleEnum {

    ADMIN(1, "管理员"),
    TEACHER(2, "教师"),
    STUDENT(3, "学生");

    private final Integer code;
    private final String desc;

    public static UserRoleEnum getByCode(Integer code) {
        for (UserRoleEnum role : values()) {
            if (role.getCode().equals(code)) {
                return role;
            }
        }
        return null;
    }
}
