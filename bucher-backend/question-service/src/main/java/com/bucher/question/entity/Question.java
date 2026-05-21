package com.bucher.question.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目实体
 */
@Data
@TableName("question")
public class Question {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long teacherId;

    private Long groupId;

    private Integer type;

    private String content;

    private String answer;

    private String analysis;

    private Integer difficulty;

    private Integer score;

    private Integer allowAttachment;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
