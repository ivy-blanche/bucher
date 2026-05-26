package com.bucher.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 未发布考试列表 VO
 */
@Data
@Schema(description = "未发布考试列表响应")
public class ExamUnpublishedVO {

    @Schema(description = "考试ID")
    private Long id;

    @Schema(description = "考试标题")
    private String title;

    @Schema(description = "课程名称")
    private String courseName;
}
