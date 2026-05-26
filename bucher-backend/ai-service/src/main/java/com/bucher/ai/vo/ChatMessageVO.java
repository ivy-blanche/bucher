package com.bucher.ai.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对话消息 VO
 */
@Data
public class ChatMessageVO {
    private String role;
    private String content;
    private LocalDateTime createTime;
}
