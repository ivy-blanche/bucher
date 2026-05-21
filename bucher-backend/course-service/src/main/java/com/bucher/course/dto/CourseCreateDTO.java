package com.bucher.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建课程请求
 */
@Data
@Schema(description = "创建课程请求")
public class CourseCreateDTO {

    @NotBlank(message = "课程名称不能为空")
    @Schema(description = "课程名称", example = "Java程序设计")
    private String name;

    @NotBlank(message = "开设学期不能为空")
    @Schema(description = "开设学期", example = "2025-2026-1")
    private String semester;

    @Schema(description = "课程描述", example = "面向大二学生的Java基础课程")
    private String description;
}
