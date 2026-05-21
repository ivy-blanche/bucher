package com.bucher.question.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改题库分组 DTO
 */
@Data
@Schema(description = "修改题库分组请求")
public class QuestionGroupUpdateDTO {

    @NotBlank(message = "题库名称不能为空")
    @Size(max = 100, message = "题库名称长度不能超过100")
    @Schema(description = "题库名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
}
