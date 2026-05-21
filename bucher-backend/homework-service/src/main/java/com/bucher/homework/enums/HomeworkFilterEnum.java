package com.bucher.homework.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 作业列表筛选模式枚举
 */
@Getter
@AllArgsConstructor
public enum HomeworkFilterEnum {

    ALL("all", "全部"),
    COLLECTING("collecting", "收集中"),
    PENDING_GRADE("pending", "待批改"),
    ENDED("ended", "已结束");

    private final String code;
    private final String desc;

    public static HomeworkFilterEnum of(String code) {
        if (code == null) return ALL;
        for (HomeworkFilterEnum value : values()) {
            if (value.code.equals(code)) return value;
        }
        return ALL;
    }
}
