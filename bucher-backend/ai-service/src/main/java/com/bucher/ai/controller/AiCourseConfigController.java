package com.bucher.ai.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bucher.ai.entity.AiCourseConfig;
import com.bucher.ai.enums.AiUserRoleEnum;
import com.bucher.ai.mapper.AiCourseConfigMapper;
import com.bucher.ai.service.AiPermissionService;
import com.bucher.common.enums.ResultCodeEnum;
import com.bucher.common.exception.BusinessException;
import com.bucher.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 课程 AI 配置接口 — 教师启用/关闭课程 AI
 */
@Tag(name = "课程 AI 配置")
@RestController
@RequestMapping("/ai/courses")
@RequiredArgsConstructor
public class AiCourseConfigController {

    private final AiCourseConfigMapper courseConfigMapper;
    private final AiPermissionService aiPermissionService;

    @Operation(summary = "启用或关闭课程 AI")
    @PostMapping("/{courseId}/config")
    public Result<Void> setCourseAiEnabled(
            @RequestHeader("X-User-Role") Integer role,
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long courseId,
            @RequestParam Boolean enabled) {
        // 管理员或已授权教师可操作
        if (!AiUserRoleEnum.ADMIN.getCode().equals(role)) {
            if (!AiUserRoleEnum.TEACHER.getCode().equals(role) || !aiPermissionService.hasPermission(userId)) {
                throw new BusinessException(ResultCodeEnum.FORBIDDEN);
            }
        }

        AiCourseConfig config = courseConfigMapper.selectOne(
                new LambdaQueryWrapper<AiCourseConfig>()
                        .eq(AiCourseConfig::getCourseId, courseId)
                        .eq(AiCourseConfig::getIsDeleted, 0)
                        .last("LIMIT 1"));

        if (config == null) {
            config = new AiCourseConfig();
            config.setCourseId(courseId);
            config.setTeacherId(userId);
            config.setStatus(enabled ? 1 : 0);
            courseConfigMapper.insert(config);
        } else {
            config.setStatus(enabled ? 1 : 0);
            config.setTeacherId(userId);
            courseConfigMapper.updateById(config);
        }
        return Result.success();
    }

    @Operation(summary = "查询课程 AI 启用状态")
    @GetMapping("/{courseId}/config")
    public Result<Boolean> getCourseAiEnabled(
            @PathVariable Long courseId) {
        AiCourseConfig config = courseConfigMapper.selectOne(
                new LambdaQueryWrapper<AiCourseConfig>()
                        .eq(AiCourseConfig::getCourseId, courseId)
                        .eq(AiCourseConfig::getIsDeleted, 0)
                        .last("LIMIT 1"));
        return Result.success(config != null && Integer.valueOf(1).equals(config.getStatus()));
    }
}
