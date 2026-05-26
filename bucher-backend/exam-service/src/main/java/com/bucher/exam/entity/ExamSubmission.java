package com.bucher.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 考试提交记录实体
 */
@Data
@TableName("exam_submission")
public class ExamSubmission {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long examId;

    private Long studentId;

    private Integer score;

    private String teacherComment;

    private LocalDateTime submitTime;

    private LocalDateTime gradeTime;

    private Integer gradeStatus;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
