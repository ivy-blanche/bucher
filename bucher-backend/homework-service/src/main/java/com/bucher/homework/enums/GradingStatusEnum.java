package com.bucher.homework.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 批改状态枚举
 */
@Getter
@AllArgsConstructor
public enum GradingStatusEnum {

    NO_NEED(0, "无需批改/全部已批"),
    PENDING(1, "待批改");

    private final Integer code;
    private final String desc;

    public static GradingStatusEnum of(Integer code) {
        if (code == null) return null;
        for (GradingStatusEnum value : values()) {
            if (value.code.equals(code)) return value;
        }
        return null;
    }
}
