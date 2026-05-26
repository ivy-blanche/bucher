package com.bucher.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教师 AI 权限 VO
 */
@Data
@Schema(description = "教师 AI 权限响应")
public class AiPermissionVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "教师用户 ID")
    private Long teacherId;

    @Schema(description = "状态：1=已授权，0=已撤销")
    private Integer status;

    @Schema(description = "授权操作的管理员 ID")
    private Long grantedBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
