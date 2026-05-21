package com.bucher.course.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 课程资料视图（不暴露 MinIO 内部路径）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "课程资料")
public class CourseMaterialVO {

    private Long id;

    private Long courseId;

    @Schema(description = "原始文件名")
    private String fileName;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "MIME类型")
    private String fileType;

    @Schema(description = "文件扩展名")
    private String fileExt;

    @Schema(description = "视频时长（秒），仅视频文件")
    private Integer duration;

    @Schema(description = "上传时间")
    private LocalDateTime createTime;
}
