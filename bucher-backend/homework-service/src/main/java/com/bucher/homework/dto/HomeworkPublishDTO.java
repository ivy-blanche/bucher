package com.bucher.homework.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 发布作业请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "发布作业请求")
public class HomeworkPublishDTO extends HomeworkDraftSaveDTO {

    @NotEmpty(message = "发布班级不能为空")
    @Schema(description = "教学班ID列表")
    private List<Long> classIds;

    @NotNull(message = "截止时间不能为空")
    @Future(message = "截止时间必须在当前时间之后")
    @Schema(description = "截止时间", example = "2026-06-20T23:59:59")
    private LocalDateTime deadline;
}
