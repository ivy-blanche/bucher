package com.bucher.message.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 通知类型枚举
 */
@Getter
@AllArgsConstructor
public enum NotificationTypeEnum {

    HOMEWORK(1, "作业通知"),
    EXAM(2, "考试通知");

    private final Integer code;
    private final String desc;

    public static NotificationTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }
}
