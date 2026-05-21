package com.bucher.course.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课程选课记录
 */
@Data
@TableName("course_enrollment")
public class CourseEnrollment {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long courseId;

    private Long studentId;

    private Integer status;  // 1=已选课，0=退选

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
