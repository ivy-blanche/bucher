package com.bucher.course.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课程班级成员实体
 */
@Data
@TableName("course_class_member")
public class CourseClassMember {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long courseClassId;

    private Long studentId;

    private Integer joinType;

    private LocalDateTime joinTime;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
