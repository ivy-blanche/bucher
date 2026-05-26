package com.bucher.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 考试作答记录实体
 */
@Data
@TableName("exam_answer")
public class ExamAnswer {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long examId;

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
