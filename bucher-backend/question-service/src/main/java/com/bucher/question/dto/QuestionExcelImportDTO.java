package com.bucher.question.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Excel 导入题目行映射
 */
@Data
@Schema(description = "Excel 导入题目行")
public class QuestionExcelImportDTO {

    @ExcelProperty("题型")
    @Schema(description = "题型：单选/多选/填空/简答")
    private String typeName;

    @ExcelProperty("题干")
    @Schema(description = "题目内容")
    private String content;

    @ExcelProperty("选项A")
    @Schema(description = "选项A内容")
    private String optionA;

    @ExcelProperty("选项B")
    @Schema(description = "选项B内容")
    private String optionB;

    @ExcelProperty("选项C")
    @Schema(description = "选项C内容")
    private String optionC;

    @ExcelProperty("选项D")
    @Schema(description = "选项D内容")
    private String optionD;

    @ExcelProperty("选项E")
    @Schema(description = "选项E内容")
    private String optionE;

    @ExcelProperty("选项F")
    @Schema(description = "选项F内容")
    private String optionF;

    @ExcelProperty("正确答案")
    @Schema(description = "正确答案（选择题填A/B/C/D，填空/简答填文本）")
    private String correctAnswer;

    @ExcelProperty("解析")
    @Schema(description = "题目解析")
    private String analysis;

    @ExcelProperty("难度")
    @Schema(description = "难度：简单/中等/困难")
    private String difficultyName;

    @ExcelProperty("分值")
    @Schema(description = "分值")
    private Integer score;
}
