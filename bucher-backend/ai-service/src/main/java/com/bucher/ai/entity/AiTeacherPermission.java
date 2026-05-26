package com.bucher.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教师 AI 使用权限实体
 */
@Data
@TableName("ai_teacher_permission")
public class AiTeacherPermission {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long teacherId;

    private Integer status;

    private Long grantedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
