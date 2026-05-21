package com.bucher.question.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 测试用例 VO
 */
@Data
@Builder
@Schema(description = "测试用例")
public class QuestionTestCaseVO {

    @Schema(description = "用例ID")
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
