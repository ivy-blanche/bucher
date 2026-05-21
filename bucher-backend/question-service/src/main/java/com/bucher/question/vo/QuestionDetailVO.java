package com.bucher.question.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 题目详情 VO
 */
@Data
@Builder
@Schema(description = "题目详情（含选项）")
public class QuestionDetailVO {

    @Schema(description = "题目ID")
    private Long id;

    @Schema(description = "题目类型 1-单选 2-多选 3-填空 4-简答")
    private Integer type;

    @Schema(description = "题干（富文本HTML）")
    private String content;

    @Schema(description = "参考答案")
    private String answer;

    @Schema(description = "解析（富文本HTML）")
    private String analysis;

    @Schema(description = "选项列表（仅选择题有值）")
    private List<QuestionOptionVO> options;

    @Schema(description = "编程题配置（type=5时有值）")
    private QuestionProgrammingVO programmingConfig;

    @Schema(description = "测试用例列表（type=5时有值）")
    private List<QuestionTestCaseVO> testCases;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
