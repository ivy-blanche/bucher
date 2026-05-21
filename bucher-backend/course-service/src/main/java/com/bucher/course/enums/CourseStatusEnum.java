package com.bucher.course.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 课程状态枚举
 */
@Getter
@AllArgsConstructor
public enum CourseStatusEnum {

    ACTIVE(1, "进行中"),
    CLOSED(0, "已结课");

    private final Integer code;
    private final String desc;

    public static CourseStatusEnum getByCode(Integer code) {
        for (CourseStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
