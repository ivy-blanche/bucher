package com.bucher.question.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 导入结果
 */
@Data
@Builder
public class QuestionImportResultVO {

    private int totalCount;

    private int successCount;

    private int failCount;

    private List<QuestionImportErrorVO> errors;
}
