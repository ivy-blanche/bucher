package com.bucher.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教师端考试列表 VO
 */
@Data
@Schema(description = "教师端考试列表响应")
public class ExamListVO {

    @Schema(description = "考试ID")
    private Long id;

    @Schema(description = "考试标题")
    private String title;

    @Schema(description = "课程名称")
    private String courseName;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "考试时长（分钟）")
    private Integer duration;

    @Schema(description = "总分")
    private Integer totalScore;

    @Schema(description = "状态: 0=未发布, 1=进行中, 2=已结束")
    private Integer status;

    @Schema(description = "已提交人数")
    private Integer submitCount;

    @Schema(description = "总人数")
    private Integer totalCount;
}
