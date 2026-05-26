package com.bucher.exam.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 考试状态枚举
 */
@Getter
@AllArgsConstructor
public enum ExamStatusEnum {

    UNPUBLISHED(0, "未发布"),
    IN_PROGRESS(1, "进行中"),
    ENDED(2, "已结束");

    private final Integer code;
    private final String desc;

    public static ExamStatusEnum of(Integer code) {
        if (code == null) return null;
        for (ExamStatusEnum value : values()) {
            if (value.code.equals(code)) return value;
        }
        return null;
    }
}
