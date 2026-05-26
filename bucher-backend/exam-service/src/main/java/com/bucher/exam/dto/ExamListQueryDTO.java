package com.bucher.exam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 考试列表查询请求
 */
@Data
@Schema(description = "考试列表查询请求")
public class ExamListQueryDTO {

    @Schema(description = "教师ID（请求头）")
    private Long teacherId;

    @Schema(description = "筛选模式: 0=全部, 1=进行中, 2=已结束")
    private String filterMode;

    @Min(value = 1, message = "页码最小为1")
    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数最小为1")
    @Schema(description = "每页条数", example = "10")
    private Integer pageSize = 10;
}
