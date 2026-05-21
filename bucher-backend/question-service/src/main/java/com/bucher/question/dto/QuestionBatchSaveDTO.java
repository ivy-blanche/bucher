package com.bucher.question.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量保存题目请求体
 */
@Data
@Schema(description = "批量保存题目请求")
public class QuestionBatchSaveDTO {

    @NotNull(message = "题库分组ID不能为空")
    @Schema(description = "题库分组ID")
    private Long groupId;

    @NotEmpty(message = "题目列表不能为空")
    @Valid
    @Schema(description = "题目列表")
    private List<QuestionSaveItem> questions;
}
