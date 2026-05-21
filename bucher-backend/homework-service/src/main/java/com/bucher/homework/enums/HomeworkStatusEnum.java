package com.bucher.homework.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 作业状态枚举
 */
@Getter
@AllArgsConstructor
public enum HomeworkStatusEnum {

    UNPUBLISHED(0, "未发布"),
    IN_PROGRESS(1, "进行中"),
    ENDED(2, "已截止");

    private final Integer code;
    private final String desc;

    public static HomeworkStatusEnum of(Integer code) {
        if (code == null) return null;
        for (HomeworkStatusEnum value : values()) {
            if (value.code.equals(code)) return value;
        }
        return null;
    }
}
