package com.bucher.homework.vo;

import lombok.Data;

/**
 * 编程题配置 VO（Feign 接收 question-service 响应）
 */
@Data
public class QuestionProgrammingVO {

    private String templateCode;

    private Integer judge0LanguageId;

    private Integer timeLimit;

    private Integer memoryLimit;
}
