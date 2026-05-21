package com.bucher.homework.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提交作业 DTO
 */
@Data
@Schema(description = "提交作业")
public class HomeworkSubmitDTO {

    @NotNull(message = "作业ID不能为空")
    @Schema(description = "作业ID")
    private Long homeworkId;
}
