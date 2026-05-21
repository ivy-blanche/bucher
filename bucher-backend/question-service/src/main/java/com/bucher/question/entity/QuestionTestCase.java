package com.bucher.question.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 编程题测试用例
 */
@Data
@TableName("question_test_case")
public class QuestionTestCase {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long questionId;

    private String input;

    private String expectedOutput;

    private Integer isSample;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
