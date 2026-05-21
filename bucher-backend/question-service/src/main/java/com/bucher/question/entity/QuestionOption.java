package com.bucher.question.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目选项实体（单选/多选）
 */
@Data
@TableName("question_option")
public class QuestionOption {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long questionId;

    private String label;

    private String content;

    private Integer isCorrect;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
