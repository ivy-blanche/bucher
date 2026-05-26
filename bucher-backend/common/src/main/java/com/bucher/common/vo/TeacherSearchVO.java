package com.bucher.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 教师搜索 VO
 */
@Data
@Schema(description = "教师搜索结果")
public class TeacherSearchVO {

    @Schema(description = "教师用户 ID")
    private Long id;

    @Schema(description = "工号")
    private String userNo;

    @Schema(description = "真实姓名")
    private String realName;
}
