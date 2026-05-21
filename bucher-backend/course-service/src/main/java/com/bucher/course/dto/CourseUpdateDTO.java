package com.bucher.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新课程请求
 */
@Data
@Schema(description = "更新课程请求")
public class CourseUpdateDTO {

    @NotNull(message = "课程ID不能为空")
    @Schema(description = "课程ID")
    private Long id;

    @NotBlank(message = "课程名称不能为空")
    @Schema(description = "课程名称")
    private String name;

    @NotBlank(message = "开设学期不能为空")
    @Schema(description = "开设学期")
    private String semester;

    @Schema(description = "课程描述")
    private String description;

    @Schema(description = "状态：1-进行中，0-已结课")
    private Integer status;
}
