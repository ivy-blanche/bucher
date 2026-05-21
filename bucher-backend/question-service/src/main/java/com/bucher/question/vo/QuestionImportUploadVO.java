package com.bucher.question.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 上传文件返回结果
 */
@Data
@Builder
public class QuestionImportUploadVO {

    private String fileKey;

    private String originalName;

    private int rowCount;
}
