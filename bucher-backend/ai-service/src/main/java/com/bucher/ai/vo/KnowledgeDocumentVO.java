package com.bucher.ai.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库文档响应 VO
 */
@Data
public class KnowledgeDocumentVO {
    private Long id;
    private Long courseId;
    private Long teacherId;
    private String teacherName;
    private String fileName;
    private Long fileSize;
    private String fileExt;
    private Integer status;
    private Integer chunkCount;
    private String errorMsg;
    private LocalDateTime createTime;
}
