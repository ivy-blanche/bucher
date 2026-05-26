package com.bucher.exam.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 题目详情 VO（Feign 接收 question-service 响应）
 */
@Data
public class QuestionDetailVO {

    private Long id;

    private Integer type;

    private String content;

    private String answer;

    private String analysis;

    private List<OptionVO> options;

    private QuestionProgrammingVO programmingConfig;

    private List<QuestionTestCaseVO> testCases;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @Data
    public static class OptionVO {

        private Long id;

        private String label;

        private String content;

        private Boolean isCorrect;

        private Integer sortOrder;
    }

    @Data
    public static class QuestionProgrammingVO {

        private String templateCode;

        private Integer judge0LanguageId;

        private Integer timeLimit;

        private Integer memoryLimit;
    }

    @Data
    public static class QuestionTestCaseVO {

        private Long id;

        private String input;

        private String expectedOutput;

        private Boolean isSample;

        private Integer sortOrder;
    }
}
