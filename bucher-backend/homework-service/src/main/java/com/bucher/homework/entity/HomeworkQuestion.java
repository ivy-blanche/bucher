package com.bucher.homework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作业题目关联实体
 */
@Data
@TableName("homework_question")
public class HomeworkQuestion {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long homeworkId;

    private Long questionId;

    private Integer score;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
