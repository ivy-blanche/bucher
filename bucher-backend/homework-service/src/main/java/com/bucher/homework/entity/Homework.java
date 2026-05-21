package com.bucher.homework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作业实体
 */
@Data
@TableName("homework")
public class Homework {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long courseId;

    private String courseName;

    private Long teacherId;

    private Long courseClassId;

    private String title;

    private String description;

    private Integer composeType;

    private Long groupId;

    private Integer totalScore;

    private LocalDateTime deadline;

    private Integer status;

    private Integer gradingStatus;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
