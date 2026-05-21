package com.bucher.homework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作业教学班关联实体
 */
@Data
@TableName("homework_class")
public class HomeworkClass {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long homeworkId;

    private Long courseClassId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
