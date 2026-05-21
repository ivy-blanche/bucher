package com.bucher.homework.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 暂存单题答案 DTO
 */
@Data
@Schema(description = "暂存单题答案")
public class AnswerSaveDTO {

    @NotNull(message = "作业ID不能为空")
    @Schema(description = "作业ID")
    private Long homeworkId;

    @NotNull(message = "题目ID不能为空")
    @Schema(description = "题目ID")
    private Long questionId;

    @Schema(description = "学生答案：选择题存A/ABC格式，填空题存JSON数组，简答存文本")
    private String answer;
}
