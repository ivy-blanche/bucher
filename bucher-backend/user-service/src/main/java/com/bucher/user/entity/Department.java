package com.bucher.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 院系/部门实体
 */
@Data
@TableName("department")
public class Department {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 院系/部门名称
     */
    private String name;

    /**
     * 业务编号（如 13、01，管理员手动填写）
     */
    private String deptCode;

    /**
     * 类型：1-学校院系，2-企业部门
     */
    private Integer type;

    /**
     * 描述
     */
    private String description;

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
