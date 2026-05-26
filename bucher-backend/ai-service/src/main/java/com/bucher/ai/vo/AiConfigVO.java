package com.bucher.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 配置 VO
 */
@Data
@Schema(description = "AI 配置响应")
public class AiConfigVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "厂商")
    private String provider;

    @Schema(description = "API 密钥（脱敏）")
    private String apiKey;

    @Schema(description = "API 端点 URL")
    private String apiEndpoint;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "Embedding 模型名称")
    private String embeddingModel;

    @Schema(description = "Embedding API 密钥（脱敏）")
    private String embeddingApiKey;

    @Schema(description = "Embedding API 端点 URL")
    private String embeddingApiEndpoint;

    @Schema(description = "最大 token 数")
    private Integer maxTokens;

    @Schema(description = "温度参数")
    private BigDecimal temperature;

    @Schema(description = "状态：1=启用，0=停用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
