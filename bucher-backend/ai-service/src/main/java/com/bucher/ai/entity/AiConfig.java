package com.bucher.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 接口配置实体
 */
@Data
@TableName("ai_config")
public class AiConfig {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String projectName;

    private String provider;

    private String apiKey;

    private String apiEndpoint;

    private String modelName;

    private String embeddingModel;

    private String embeddingApiKey;

    private String embeddingApiEndpoint;

    private Integer maxTokens;

    private BigDecimal temperature;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
