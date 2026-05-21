package com.bucher.homework.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 作业列表查询 DTO
 */
@Data
@Schema(description = "作业列表查询参数")
public class HomeworkListQueryDTO {

    @Schema(description = "教师ID（从请求头获取）", hidden = true)
    private Long teacherId;

    @Schema(description = "筛选模式: all=全部, collecting=收集中, pending=待批改, ended=已结束", example = "all")
    private String filterMode = "all";

    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "10")
    private Integer pageSize = 10;
}
