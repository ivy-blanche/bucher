package com.bucher.homework.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学生端作业列表 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "学生端作业列表响应")
public class StudentHomeworkVO {

    @Schema(description = "作业ID")
    private Long id;

    @Schema(description = "作业名称")
    private String title;

    @Schema(description = "截止时间")
    private LocalDateTime deadline;

    @Schema(description = "学生端作业状态: 0=未提交(未到期), 1=已提交, 2=已被批改, 3=已过期")
    private Integer status;
}
