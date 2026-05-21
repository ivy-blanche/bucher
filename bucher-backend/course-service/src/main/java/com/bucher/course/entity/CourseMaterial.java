package com.bucher.course.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课程资料实体
 */
@Data
@TableName("course_material")
public class CourseMaterial {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long courseId;

    private Long teacherId;

    private String fileName;

    private Long fileSize;

    private String fileType;

    private String fileExt;

    private String objectName;

    private String bucketName;

    private Integer duration;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
