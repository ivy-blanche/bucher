package com.bucher.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建课程班级请求
 */
@Data
@Schema(description = "创建课程班级请求")
public class CourseClassCreateDTO {

    @NotNull(message = "课程ID不能为空")
    @Schema(description = "课程ID")
    private Long courseId;

    @NotBlank(message = "班级名称不能为空")
    @Schema(description = "班级名称", example = "1班")
    private String name;
}
