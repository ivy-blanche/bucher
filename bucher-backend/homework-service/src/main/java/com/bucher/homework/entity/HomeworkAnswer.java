package com.bucher.homework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作业作答记录（学生每道题的答案）
 */
@Data
@TableName("homework_answer")
public class HomeworkAnswer {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long homeworkId;

    private Long studentId;

    private Long questionId;

    private String answer;

    private Integer score;

    private Integer isCorrect;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
