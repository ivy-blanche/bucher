package com.bucher.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * AI 配置 DTO
 */
@Data
@Schema(description = "AI 配置请求参数")
public class AiConfigDTO {

    @Schema(description = "项目名称")
    private String projectName;

    @NotBlank(message = "厂商不能为空")
    @Schema(description = "厂商：DEEPSEEK / OPENAI / QIANFAN 等", requiredMode = Schema.RequiredMode.REQUIRED)
    private String provider;

    @NotBlank(message = "API 密钥不能为空")
    @Schema(description = "API 密钥", requiredMode = Schema.RequiredMode.REQUIRED)
    private String apiKey;

    @NotBlank(message = "API 端点 URL 不能为空")
    @Schema(description = "API 端点 URL", requiredMode = Schema.RequiredMode.REQUIRED)
    private String apiEndpoint;

    @NotBlank(message = "模型名称不能为空")
    @Schema(description = "模型名称，如 deepseek-chat", requiredMode = Schema.RequiredMode.REQUIRED)
    private String modelName;

    @NotBlank(message = "Embedding 模型名称不能为空")
    @Schema(description = "Embedding 模型名称，如 deepseek-embedding", requiredMode = Schema.RequiredMode.REQUIRED)
    private String embeddingModel;

    @Schema(description = "Embedding API 密钥（留空则复用 apiKey）")
    private String embeddingApiKey;

    @Schema(description = "Embedding API 端点 URL（留空则复用 apiEndpoint）")
    private String embeddingApiEndpoint;

    @NotNull(message = "最大 token 数不能为空")
    @Schema(description = "最大 token 数", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer maxTokens;

    @NotNull(message = "温度参数不能为空")
    @DecimalMin(value = "0.00", message = "温度参数最小为 0")
    @DecimalMax(value = "2.00", message = "温度参数最大为 2")
    @Schema(description = "温度参数 0-2", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal temperature;

    @Schema(description = "状态：1=启用，0=停用")
    private Integer status;
}
