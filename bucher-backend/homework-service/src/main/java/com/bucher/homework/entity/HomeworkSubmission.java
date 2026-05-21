package com.bucher.homework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作业提交记录实体
 */
@Data
@TableName("homework_submission")
public class HomeworkSubmission {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long homeworkId;

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
