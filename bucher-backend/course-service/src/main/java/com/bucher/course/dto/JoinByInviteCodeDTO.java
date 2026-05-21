package com.bucher.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 邀请码加入请求
 */
@Data
@Schema(description = "邀请码加入请求")
public class JoinByInviteCodeDTO {

    @NotBlank(message = "邀请码不能为空")
    @Schema(description = "邀请码", example = "A1B2C3D4")
    private String inviteCode;
}
