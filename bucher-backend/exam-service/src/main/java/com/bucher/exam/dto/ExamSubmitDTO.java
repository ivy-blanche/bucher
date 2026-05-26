package com.bucher.exam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提交考试请求
 */
@Data
@Schema(description = "提交考试请求")
public class ExamSubmitDTO {

    @NotNull(message = "考试ID不能为空")
    @Schema(description = "考试ID")
    private Long examId;
}
