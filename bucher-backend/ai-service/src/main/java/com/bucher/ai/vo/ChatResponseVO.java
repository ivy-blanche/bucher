package com.bucher.ai.vo;

import lombok.Data;

import java.util.List;

/**
 * 对话响应 VO
 */
@Data
public class ChatResponseVO {
    private String content;
    private List<SourceInfo> sources;

    @Data
    public static class SourceInfo {
        private String fileName;
        private Double score;
    }
}
