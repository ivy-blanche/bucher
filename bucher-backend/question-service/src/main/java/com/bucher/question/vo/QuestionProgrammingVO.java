package com.bucher.question.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 编程题配置 VO
 */
@Data
@Builder
@Schema(description = "编程题配置")
public class QuestionProgrammingVO {

    @Schema(description = "代码模板")
    private String templateCode;

    @Schema(description = "Judge0语言ID")
    private Integer judge0LanguageId;

    @Schema(description = "时间限制（毫秒）")
    private Integer timeLimit;

    @Schema(description = "内存限制（KB）")
    private Integer memoryLimit;
}
