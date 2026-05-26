package com.bucher.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 学生端 — 去做考试页面 VO
 */
@Data
@Schema(description = "考试做题页面数据")
public class ExamDoVO {

    @Schema(description = "考试ID")
    private Long examId;

    @Schema(description = "考试标题")
    private String title;

    @Schema(description = "考试说明")
    private String description;

    @Schema(description = "课程名称")
    private String courseName;

    @Schema(description = "总分")
    private Integer totalScore;

    @Schema(description = "考试时长（分钟）")
    private Integer duration;

    @Schema(description = "最早交卷时间（分钟）")
    private Integer earlySubmitMinutes;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "考试状态 1=进行中 2=已结束")
    private Integer status;

    @Schema(description = "是否已提交")
    private Boolean submitted;

    @Schema(description = "剩余秒数（负数表示已超时）")
    private Long remainingSeconds;

    @Schema(description = "题目列表")
    private List<QuestionVO> questions;

    @Data
    @Schema(description = "题目项")
    public static class QuestionVO {

        @Schema(description = "题目ID")
        private Long questionId;

        @Schema(description = "题目类型 single/multiple/fill/short-answer")
        private String questionType;

        @Schema(description = "题干（富文本HTML）")
        private String content;

        @Schema(description = "本题分值")
        private Integer score;

        @Schema(description = "排序序号")
        private Integer sortOrder;

        @Schema(description = "学生已填答案")
        private String answer;

        @Schema(description = "选项列表")
        private List<OptionVO> options;

        @Schema(description = "编程题模板代码")
        private String templateCode;

        @Schema(description = "编程题示例测试用例")
        private List<TestCaseVO> testCases;
    }

    @Data
    @Schema(description = "编程题测试用例")
    public static class TestCaseVO {

        @Schema(description = "输入")
        private String input;

        @Schema(description = "预期输出")
        private String expectedOutput;
    }

    @Data
    @Schema(description = "选项项（不包含正确答案标识）")
    public static class OptionVO {

        @Schema(description = "选项标签 A/B/C/D")
        private String label;

        @Schema(description = "选项内容（富文本HTML）")
        private String content;
    }
}
