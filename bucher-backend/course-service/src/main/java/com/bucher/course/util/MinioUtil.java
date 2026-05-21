package com.bucher.course.util;

import com.bucher.course.config.MinioConfig;
import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 操作工具类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinioUtil {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @PostConstruct
    public void init() {
        ensureBucketExists();
    }

    /**
     * 检查并创建 bucket
     */
    public void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(minioConfig.getBucketName()).build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(minioConfig.getBucketName()).build()
                );
                log.info("MinIO bucket 创建成功: {}", minioConfig.getBucketName());
            }
        } catch (Exception e) {
            log.error("MinIO bucket 初始化失败: {}", e.getMessage());
        }
    }

    /**
     * 上传文件
     */
    public void putObject(String objectName, InputStream inputStream, String contentType, long size) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );
            log.info("MinIO 上传成功: bucket={}, object={}", minioConfig.getBucketName(), objectName);
        } catch (Exception e) {
            log.error("MinIO 上传失败: {}", e.getMessage());
            throw new RuntimeException("文件上传到 MinIO 失败", e);
        }
    }

    /**
     * 获取文件流
     */
    public InputStream getObject(String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("MinIO 获取文件失败: {}", e.getMessage());
            throw new RuntimeException("文件获取失败", e);
        }
    }

    /**
     * 删除文件
     */
    public void removeObject(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .build()
            );
            log.info("MinIO 删除成功: object={}", objectName);
        } catch (Exception e) {
            log.error("MinIO 删除失败: {}", e.getMessage());
            throw new RuntimeException("文件删除失败", e);
        }
    }

    /**
     * 获取预签名下载 URL（临时有效）
     */
    public String getPresignedObjectUrl(String objectName, int expiryMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .method(Method.GET)
                            .expiry(expiryMinutes, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            log.error("MinIO 生成预签名URL失败: {}", e.getMessage());
            throw new RuntimeException("下载链接生成失败", e);
        }
    }
}
