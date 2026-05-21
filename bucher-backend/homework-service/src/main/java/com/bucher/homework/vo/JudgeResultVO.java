package com.bucher.homework.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 判题结果 VO
 */
@Data
@Builder
@Schema(description = "判题结果")
public class JudgeResultVO {

    @Schema(description = "作答记录ID（homework_answer.id）")
    private Long answerId;

    @Schema(description = "判题记录ID（homework_answer_judge.id）")
    private Long judgeId;

    @Schema(description = "判题状态: 0=待判题, 1=判题中, 2=已完成, 3=失败")
    private Integer status;

    @Schema(description = "得分")
    private Integer score;

    @Schema(description = "是否全部通过")
    private Boolean passed;

    @Schema(description = "通过测试用例数")
    private Integer passedCount;

    @Schema(description = "总测试用例数")
    private Integer totalCount;

    @Schema(description = "编译输出（编译错误时）")
    private String compileOutput;

    @Schema(description = "各测试用例结果")
    private List<TestCaseResult> testCaseResults;

    @Data
    @Builder
    @Schema(description = "单个测试用例结果")
    public static class TestCaseResult {

        @Schema(description = "测试用例ID")
        private Long testCaseId;

        @Schema(description = "是否通过")
        private Boolean passed;

        @Schema(description = "Judge0 状态ID: 3=Accepted, 4=Wrong Answer, 5=TLE, 6=CE, 7-12=RE")
        private Integer judgeStatusId;

        @Schema(description = "Judge0 状态描述")
        private String judgeStatusDesc;

        @Schema(description = "程序输出")
        private String stdout;

        @Schema(description = "期望输出")
        private String expectedOutput;

        @Schema(description = "运行时间（秒）")
        private String time;

        @Schema(description = "运行内存（KB）")
        private Long memory;
    }
}
