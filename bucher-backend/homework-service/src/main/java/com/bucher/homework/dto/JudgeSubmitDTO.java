package com.bucher.homework.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 编程题提交判题请求
 */
@Data
@Schema(description = "编程题提交判题请求")
public class JudgeSubmitDTO {

    @NotNull(message = "作业ID不能为空")
    @Schema(description = "作业ID")
    private Long homeworkId;

    @NotNull(message = "题目ID不能为空")
    @Schema(description = "题目ID")
    private Long questionId;

    @NotBlank(message = "代码不能为空")
    @Schema(description = "学生编写的代码")
    private String code;
}
