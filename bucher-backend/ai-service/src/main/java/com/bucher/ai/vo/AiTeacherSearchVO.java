package com.bucher.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 教师搜索结果 VO（含授权状态）
 */
@Data
@Schema(description = "AI 教师搜索结果")
public class AiTeacherSearchVO {

    @Schema(description = "教师用户 ID")
    private Long id;

    @Schema(description = "工号")
    private String userNo;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "是否已授权 AI 权限")
    private Boolean granted;
}
