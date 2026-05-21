package com.bucher.question.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 单题保存项
 */
@Data
@Schema(description = "单题保存项")
public class QuestionSaveItem {

    @Schema(description = "题目ID（新增时为null）")
    private Long id;

    @NotNull(message = "题目类型不能为空")
    @Schema(description = "题目类型 1-单选 2-多选 3-填空 4-简答")
    private Integer type;

    @NotBlank(message = "题干不能为空")
    @Schema(description = "题干（富文本HTML）")
    private String content;

    @Schema(description = "参考答案（填空/简答使用）")
    private String answer;

    @Schema(description = "解析（富文本HTML）")
    private String analysis;

    @Valid
    @Schema(description = "选项列表（选择题必填）")
    private List<QuestionOptionSaveItem> options;

    @Schema(description = "是否删除（true=删除该题）")
    private Boolean deleted;

    @Schema(description = "难度 1-简单 2-中等 3-困难")
    private Integer difficulty;

    @Schema(description = "分值")
    private Integer score;

    @Schema(description = "编程题配置（type=5时必填）")
    private ProgrammingConfigDTO programmingConfig;

    @Schema(description = "测试用例列表（type=5时必填）")
    private List<TestCaseDTO> testCases;
}
