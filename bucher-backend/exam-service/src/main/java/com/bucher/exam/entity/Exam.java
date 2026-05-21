package com.bucher.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 考试实体
 */
@Data
@TableName("exam")
public class Exam {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long courseId;

    private Long teacherId;

    private Long courseClassId;

    private String title;

    private String description;

    private Integer composeType;

    private Long groupId;

    private Integer duration;

    private Integer totalScore;

    private Integer passScore;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
