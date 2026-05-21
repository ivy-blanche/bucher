package com.bucher.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发送验证码请求 DTO
 */
@Data
public class SendCodeDTO {

    /**
     * 邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 类型：register-注册，reset-重置密码
     */
    @NotBlank(message = "类型不能为空")
    private String type;
}
