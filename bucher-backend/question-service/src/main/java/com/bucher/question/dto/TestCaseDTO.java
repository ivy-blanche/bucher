package com.bucher.question.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 测试用例
 */
@Data
@Schema(description = "测试用例")
public class TestCaseDTO {

    @Schema(description = "用例ID（新增时为null）")
    private Long id;

    @Schema(description = "测试输入")
    private String input;

    @Schema(description = "期望输出")
    private String expectedOutput;

    @Schema(description = "是否样例（true=学生可见）")
    private Boolean isSample;

    @Schema(description = "排序序号")
    private Integer sortOrder;
}
