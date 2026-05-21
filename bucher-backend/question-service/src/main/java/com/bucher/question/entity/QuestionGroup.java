package com.bucher.question.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题库分组实体
 */
@Data
@TableName("question_group")
public class QuestionGroup {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long teacherId;

    private String name;

    private String description;

    private Integer sortOrder;

    private Integer questionCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
