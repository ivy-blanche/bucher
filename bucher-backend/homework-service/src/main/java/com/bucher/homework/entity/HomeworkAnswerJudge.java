package com.bucher.homework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 编程题判题结果
 */
@Data
@TableName("homework_answer_judge")
public class HomeworkAnswerJudge {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long answerId;

    private Long questionId;

    private String judge0Token;

    private Integer status;

    private String result;

    private String compileOutput;

    private Integer score;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
