package com.bucher.question.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 选项 VO
 */
@Data
@Builder
@Schema(description = "选项信息")
public class QuestionOptionVO {

    @Schema(description = "选项ID")
    private Long id;

    @Schema(description = "选项标签（如 A、B、C、D）")
    private String label;

    @Schema(description = "选项内容（富文本HTML）")
    private String content;

    @Schema(description = "是否正确答案")
    private Boolean isCorrect;

    @Schema(description = "排序序号")
    private Integer sortOrder;
}
