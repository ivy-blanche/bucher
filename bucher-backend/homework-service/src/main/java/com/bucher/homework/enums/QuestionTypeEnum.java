package com.bucher.homework.enums;

import lombok.Getter;

/**
 * 题目类型枚举：数据库存数字，VO 返回前端字符串
 */
@Getter
public enum QuestionTypeEnum {

    SINGLE(1, "single", "单选"),
    MULTIPLE(2, "multiple", "多选"),
    FILL(3, "fill", "填空"),
    SHORT_ANSWER(4, "short-answer", "简答"),
    PROGRAMMING(5, "programming", "编程");

    private final int code;
    private final String value;
    private final String label;

    QuestionTypeEnum(int code, String value, String label) {
        this.code = code;
        this.value = value;
        this.label = label;
    }

    /**
     * 将数据库数字类型转换为前端字符串类型
     */
    public static String toValue(Integer code) {
        if (code == null) {
            return null;
        }
        for (QuestionTypeEnum type : values()) {
            if (type.code == code) {
                return type.value;
            }
        }
        return null;
    }
}
