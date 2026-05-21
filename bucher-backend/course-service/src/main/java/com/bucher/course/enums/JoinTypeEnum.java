package com.bucher.course.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 加入班级方式枚举
 */
@Getter
@AllArgsConstructor
public enum JoinTypeEnum {

    INVITE_CODE(1, "邀请码加入"),
    TEACHER_ADD(2, "教师添加");

    private final Integer code;
    private final String desc;
}
