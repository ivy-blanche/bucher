package com.bucher.question.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 选项保存项
 */
@Data
@Schema(description = "选项保存项")
public class QuestionOptionSaveItem {

    @Schema(description = "选项ID（新增时为null）")
    private Long id;

    @NotBlank(message = "选项标签不能为空")
    @Schema(description = "选项标签（如 A、B、C、D）")
    private String label;

    @NotBlank(message = "选项内容不能为空")
    @Schema(description = "选项内容（富文本HTML）")
    private String content;

    @Schema(description = "是否正确答案")
    private Boolean isCorrect;

    @Schema(description = "排序序号")
    private Integer sortOrder;
}
