package com.bucher.exam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 保存考试草稿请求
 */
@Data
@Schema(description = "保存考试草稿请求")
public class ExamDraftSaveDTO {

    @NotBlank(message = "考试标题不能为空")
    @Schema(description = "考试标题", example = "期中考试")
    private String title;

    @NotNull(message = "课程ID不能为空")
    @Schema(description = "课程ID")
    private Long courseId;

    @NotNull(message = "来源题库分组ID不能为空")
    @Schema(description = "来源题库分组ID")
    private Long sourceBankId;

    @NotEmpty(message = "题目列表不能为空")
    @Valid
    @Schema(description = "题目列表")
    private List<QuestionItem> questions;

    @Data
    @Schema(description = "题目项")
    public static class QuestionItem {

        @NotNull(message = "题目ID不能为空")
        @Schema(description = "题目ID")
        private Long questionId;

        @NotNull(message = "题目分值不能为空")
        @Schema(description = "题目分值")
        private Integer score;
    }
}
