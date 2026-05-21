package com.bucher.question.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 编程题配置
 */
@Data
@TableName("question_programming")
public class QuestionProgramming {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long questionId;

    private String templateCode;

    private Integer judge0LanguageId;

    private Integer timeLimit;

    private Integer memoryLimit;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
