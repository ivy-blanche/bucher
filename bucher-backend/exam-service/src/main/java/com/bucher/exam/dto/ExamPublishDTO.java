package com.bucher.exam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 发布考试请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "发布考试请求")
public class ExamPublishDTO extends ExamDraftSaveDTO {

    @NotEmpty(message = "发布班级不能为空")
    @Schema(description = "教学班ID列表")
    private List<Long> classIds;

    @NotNull(message = "开始时间不能为空")
    @Future(message = "开始时间必须在当前时间之后")
    @Schema(description = "开始时间", example = "2026-06-01T09:00:00")
    private LocalDateTime startTime;

    @NotNull(message = "考试时长不能为空")
    @Min(value = 1, message = "考试时长至少为1分钟")
    @Schema(description = "考试时长（分钟）", example = "120")
    private Integer duration;

    @NotNull(message = "最早交卷时间不能为空")
    @Min(value = 0, message = "最早交卷时间不能为负数")
    @Schema(description = "最早交卷时间（分钟），开考后多久可以交卷", example = "30")
    private Integer earlySubmitMinutes;

    @Schema(description = "最晚入场时间（分钟），开考后多久禁止进入，默认15", example = "15")
    private Integer lateBanMinutes;

    @Schema(description = "是否自动提交: 1=超时自动提交, 0=不自动提交", example = "1")
    private Integer autoSubmit;

    @Schema(description = "及格分")
    private Integer passScore;

    @Schema(description = "考试说明")
    private String description;
}
