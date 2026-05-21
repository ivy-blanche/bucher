package com.bucher.question.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 执行导入请求体
 */
@Data
@Schema(description = "执行导入请求")
public class QuestionImportExecuteDTO {

    @NotNull(message = "分组ID不能为空")
    @Schema(description = "题库分组ID")
    private Long groupId;

    @NotBlank(message = "文件Key不能为空")
    @Schema(description = "上传文件返回的fileKey")
    private String fileKey;
}
