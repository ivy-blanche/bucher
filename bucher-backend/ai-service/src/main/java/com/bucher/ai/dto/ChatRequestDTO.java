package com.bucher.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 对话请求 DTO
 */
@Data
public class ChatRequestDTO {
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 5000, message = "消息内容过长")
    private String message;
}
