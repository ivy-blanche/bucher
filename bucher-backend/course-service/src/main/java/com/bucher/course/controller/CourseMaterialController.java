package com.bucher.course.controller;

import com.bucher.common.result.Result;
import com.bucher.course.service.CourseMaterialService;
import com.bucher.course.vo.CourseMaterialVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 课程资料控制器
 */
@Tag(name = "课程资料管理")
@RestController
@RequestMapping("/course/material")
@RequiredArgsConstructor
public class CourseMaterialController {

    private final CourseMaterialService courseMaterialService;

    @Operation(summary = "上传课程资料")
    @PostMapping("/upload/{courseId}")
    public Result<CourseMaterialVO> upload(
            @Parameter(description = "课程ID") @PathVariable Long courseId,
            @Parameter(description = "上传文件") @RequestParam("file") MultipartFile file,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") Integer role) {
        return Result.success(courseMaterialService.uploadMaterial(courseId, file, userId, role));
    }

    @Operation(summary = "获取课程资料列表")
    @GetMapping("/list/{courseId}")
    public Result<List<CourseMaterialVO>> list(
            @Parameter(description = "课程ID") @PathVariable Long courseId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") Integer role) {
        return Result.success(courseMaterialService.listMaterials(courseId, userId, role));
    }

    @Operation(summary = "下载课程资料")
    @GetMapping("/download/{materialId}")
    public void download(
            @Parameter(description = "资料ID") @PathVariable Long materialId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") Integer role,
            HttpServletResponse response) {
        courseMaterialService.downloadMaterial(materialId, userId, role, response);
    }

    @Operation(summary = "批量下载课程资料（ZIP打包）")
    @GetMapping("/batch-download")
    public void batchDownload(
            @Parameter(description = "资料ID列表，逗号分隔") @RequestParam("materialIds") List<Long> materialIds,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") Integer role,
            HttpServletResponse response) {
        courseMaterialService.batchDownloadMaterials(materialIds, userId, role, response);
    }

    @Operation(summary = "删除课程资料")
    @DeleteMapping("/{materialId}")
    public Result<Void> delete(
            @Parameter(description = "资料ID") @PathVariable Long materialId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") Integer role) {
        courseMaterialService.deleteMaterial(materialId, userId, role);
        return Result.success();
    }
}
