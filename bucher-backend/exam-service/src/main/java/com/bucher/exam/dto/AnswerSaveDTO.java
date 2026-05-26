package com.bucher.exam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 暂存答案请求
 */
@Data
@Schema(description = "暂存答案请求")
public class AnswerSaveDTO {

    @NotNull(message = "考试ID不能为空")
    @Schema(description = "考试ID")
    private Long examId;

    @NotNull(message = "题目ID不能为空")
    @Schema(description = "题目ID")
    private Long questionId;

    @Schema(description = "学生答案")
    private String answer;
}
