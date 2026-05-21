package com.bucher.question.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 编程题配置
 */
@Data
@Schema(description = "编程题配置")
public class ProgrammingConfigDTO {

    @Schema(description = "代码模板")
    private String templateCode;

    @Schema(description = "Judge0语言ID")
    private Integer judge0LanguageId;

    @Schema(description = "时间限制（毫秒），默认3000")
    private Integer timeLimit;

    @Schema(description = "内存限制（KB），默认256000")
    private Integer memoryLimit;
}
