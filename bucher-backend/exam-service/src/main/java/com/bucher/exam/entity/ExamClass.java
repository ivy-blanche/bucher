package com.bucher.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 考试教学班关联实体
 */
@Data
@TableName("exam_class")
public class ExamClass {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long examId;

    private Long courseClassId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
