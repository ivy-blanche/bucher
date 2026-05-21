package com.bucher.homework.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 作业列表 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "作业列表响应")
public class HomeworkListVO {

    @Schema(description = "作业ID")
    private Long id;

    @Schema(description = "作业名称")
    private String title;

    @Schema(description = "课程名称")
    private String courseName;

    @Schema(description = "截止日期")
    private LocalDateTime deadline;

    @Schema(description = "提交人数")
    private Integer submitCount;

    @Schema(description = "已批改人数")
    private Integer gradedCount;

    @Schema(description = "作业状态: 0=未发布, 1=进行中, 2=已截止")
    private Integer status;

    @Schema(description = "批改状态: 0=无需批改/全部已批, 1=待批改")
    private Integer gradingStatus;
}
