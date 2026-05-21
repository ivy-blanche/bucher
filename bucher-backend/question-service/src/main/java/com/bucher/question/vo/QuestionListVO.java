package com.bucher.question.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目列表 VO
 */
@Data
@Builder
@Schema(description = "题目列表项")
public class QuestionListVO {

    @Schema(description = "题目ID")
    private Long id;

    @Schema(description = "题干预览（截取前100字）")
    private String contentPreview;

    @Schema(description = "题目类型 1-单选 2-多选 3-填空 4-简答")
    private Integer type;

    @Schema(description = "题目类型名称")
    private String typeName;

    @Schema(description = "难度 1-简单 2-中等 3-困难")
    private Integer difficulty;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
