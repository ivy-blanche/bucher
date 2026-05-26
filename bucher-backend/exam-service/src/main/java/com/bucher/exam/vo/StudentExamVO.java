package com.bucher.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学生端考试列表 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "学生端考试列表响应")
public class StudentExamVO {

    @Schema(description = "考试ID")
    private Long id;

    @Schema(description = "考试名称")
    private String title;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "考试时长（分钟）")
    private Integer duration;

    @Schema(description = "学生端考试状态: 0=未开始, 1=进行中(未提交), 2=已提交, 3=已结束")
    private Integer status;
}
