package com.bucher.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 行政班级实体
 */
@Data
@TableName("admin_class")
public class AdminClass {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 行政班级名称
     */
    private String name;

    /**
     * 所属院系/部门ID
     */
    private Long deptId;

    /**
     * 入学年份
     */
    private Integer year;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    @TableLogic
    private Integer isDeleted;
}
