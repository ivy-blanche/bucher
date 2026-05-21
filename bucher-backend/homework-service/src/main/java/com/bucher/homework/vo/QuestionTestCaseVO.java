package com.bucher.homework.vo;

import lombok.Data;

/**
 * 测试用例 VO（Feign 接收 question-service 响应）
 */
@Data
public class QuestionTestCaseVO {

    private Long id;

    private String input;

    private String expectedOutput;

    private Boolean isSample;

    private Integer sortOrder;
}
