package com.bucher.course.constant;

import java.util.Set;

/**
 * 资料模块常量
 */
public interface MaterialConstants {

    /** 默认视频时长（预留字段，暂不实现检测） */
    int DEFAULT_DURATION = 0;

    /** 下载方式切换阈值：超过此大小的文件使用预签名URL（100MB） */
    long PRESIGNED_URL_THRESHOLD = 100 * 1024 * 1024L;

    /** 预签名URL有效期（分钟） */
    int PRESIGNED_URL_EXPIRY = 15;

    /** 允许的文件扩展名 */
    Set<String> ALLOWED_EXTENSIONS = Set.of(
            // 文档
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt",
            // 图片
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg",
            // 视频
            "mp4", "avi", "mov", "wmv", "flv", "mkv", "webm",
            // 音频
            "mp3", "wav", "aac", "ogg",
            // 压缩包
            "zip", "rar", "7z"
    );

    /** 视频扩展名集合 */
    Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "avi", "mov", "wmv", "flv", "mkv", "webm");
}
