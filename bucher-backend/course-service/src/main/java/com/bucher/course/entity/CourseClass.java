package com.bucher.course.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课程班级实体
 */
@Data
@TableName("course_class")
public class CourseClass {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long courseId;

    private String name;

    private Long teacherId;

    private String inviteCode;

    private LocalDateTime inviteExpireTime;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
