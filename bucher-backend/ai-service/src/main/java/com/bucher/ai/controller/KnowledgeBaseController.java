package com.bucher.ai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bucher.ai.enums.AiUserRoleEnum;
import com.bucher.ai.service.AiPermissionService;
import com.bucher.ai.service.KnowledgeBaseService;
import com.bucher.ai.vo.KnowledgeDocumentVO;
import com.bucher.common.enums.ResultCodeEnum;
import com.bucher.common.exception.BusinessException;
import com.bucher.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识库文档管理接口 — 教师管理课程文档，管理员管理全局文档
 */
@Tag(name = "AI 知识库管理")
@RestController
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final AiPermissionService aiPermissionService;

    // ========== 教师端：课程知识库 ==========

    @Operation(summary = "上传课程文档")
    @PostMapping("/ai/knowledge-base/courses/{courseId}/documents")
    public Result<KnowledgeDocumentVO> uploadCourseDocument(
            @RequestHeader("X-User-Role") Integer role,
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long courseId,
            @RequestParam("file") MultipartFile file) {
        checkTeacherOrAdmin(role, userId);
        return Result.success(knowledgeBaseService.upload(courseId, userId, file));
    }

    @Operation(summary = "查询课程文档列表")
    @GetMapping("/ai/knowledge-base/courses/{courseId}/documents")
    public Result<Page<KnowledgeDocumentVO>> listCourseDocuments(
            @RequestHeader("X-User-Role") Integer role,
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        checkTeacherOrAdmin(role, userId);
        return Result.success(knowledgeBaseService.listByCourse(courseId, userId, page, size));
    }

    @Operation(summary = "删除课程文档")
    @DeleteMapping("/ai/knowledge-base/courses/{courseId}/documents/{id}")
    public Result<Void> deleteCourseDocument(
            @RequestHeader("X-User-Role") Integer role,
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long courseId,
            @PathVariable Long id) {
        checkTeacherOrAdmin(role, userId);
        knowledgeBaseService.delete(id, userId, false);
        return Result.success();
    }

    // ========== 管理端：全局知识库 ==========

    @Operation(summary = "上传全局文档")
    @PostMapping("/ai/admin/knowledge-base/documents")
    public Result<KnowledgeDocumentVO> uploadGlobalDocument(
            @RequestHeader("X-User-Role") Integer role,
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam("file") MultipartFile file) {
        checkAdmin(role);
        return Result.success(knowledgeBaseService.upload(0L, userId, file));
    }

    @Operation(summary = "查询全局文档列表")
    @GetMapping("/ai/admin/knowledge-base/documents")
    public Result<Page<KnowledgeDocumentVO>> listGlobalDocuments(
            @RequestHeader("X-User-Role") Integer role,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        checkAdmin(role);
        return Result.success(knowledgeBaseService.listGlobal(page, size));
    }

    @Operation(summary = "删除全局文档")
    @DeleteMapping("/ai/admin/knowledge-base/documents/{id}")
    public Result<Void> deleteGlobalDocument(
            @RequestHeader("X-User-Role") Integer role,
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        checkAdmin(role);
        knowledgeBaseService.delete(id, userId, true);
        return Result.success();
    }

    // ========== 权限校验 ==========

    private void checkAdmin(Integer role) {
        if (!AiUserRoleEnum.ADMIN.getCode().equals(role)) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN);
        }
    }

    private void checkTeacherOrAdmin(Integer role, Long userId) {
        if (AiUserRoleEnum.ADMIN.getCode().equals(role)) return;
        if (AiUserRoleEnum.TEACHER.getCode().equals(role) && aiPermissionService.hasPermission(userId)) return;
        throw new BusinessException(ResultCodeEnum.FORBIDDEN);
    }
}
