package com.bucher.question.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 导入错误行信息
 */
@Data
@Builder
public class QuestionImportErrorVO {

    private int row;

    private String reason;
}
