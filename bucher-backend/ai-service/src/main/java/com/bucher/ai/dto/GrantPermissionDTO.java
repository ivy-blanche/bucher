package com.bucher.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 授权教师 DTO
 */
@Data
@Schema(description = "授权教师请求参数")
public class GrantPermissionDTO {

    @NotNull(message = "教师 ID 不能为空")
    @Schema(description = "教师用户 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long teacherId;
}
