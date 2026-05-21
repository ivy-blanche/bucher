package com.bucher.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 考试题目关联实体
 */
@Data
@TableName("exam_question")
public class ExamQuestion {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long examId;

    private Long questionId;

    private Integer score;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
