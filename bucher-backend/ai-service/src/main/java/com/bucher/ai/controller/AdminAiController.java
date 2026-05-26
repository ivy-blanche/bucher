package com.bucher.ai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bucher.ai.dto.AiConfigDTO;
import com.bucher.ai.dto.GrantPermissionDTO;
import com.bucher.ai.service.AiConfigService;
import com.bucher.ai.service.AiPermissionService;
import com.bucher.ai.vo.AiConfigVO;
import com.bucher.ai.vo.AiPermissionVO;
import com.bucher.ai.vo.AiTeacherSearchVO;
import com.bucher.common.enums.ResultCodeEnum;
import com.bucher.common.exception.BusinessException;
import com.bucher.common.result.Result;
import com.bucher.ai.enums.AiUserRoleEnum;
import com.bucher.ai.feign.UserFeignClient;
import com.bucher.common.vo.TeacherSearchVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.*;

/**
 * AI 管理后台接口
 */
@Tag(name = "AI 管理后台接口")
@RestController
@RequestMapping("/ai/admin")
@RequiredArgsConstructor
public class AdminAiController {

    private final AiConfigService aiConfigService;
    private final AiPermissionService aiPermissionService;
    private final UserFeignClient userFeignClient;

    private void checkAdmin(Integer role) {
        if (role == null || !AiUserRoleEnum.ADMIN.getCode().equals(role)) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN);
        }
    }

    @Operation(summary = "保存 AI 配置")
    @PostMapping("/config")
    public Result<Void> saveConfig(
            @RequestHeader("X-User-Role") Integer role,
            @Valid @RequestBody AiConfigDTO dto) {
        checkAdmin(role);
        aiConfigService.save(dto);
        return Result.success();
    }

    @Operation(summary = "获取 AI 配置")
    @GetMapping("/config")
    public Result<AiConfigVO> getConfig(
            @RequestHeader("X-User-Role") Integer role) {
        checkAdmin(role);
        return Result.success(aiConfigService.get());
    }

    @Operation(summary = "授权教师使用 AI")
    @PostMapping("/permissions/{teacherId}")
    public Result<Void> grantPermission(
            @RequestHeader("X-User-Role") Integer role,
            @RequestHeader("X-User-Id") Long adminId,
            @PathVariable Long teacherId) {
        checkAdmin(role);
        aiPermissionService.grant(teacherId, adminId);
        return Result.success();
    }

    @Operation(summary = "撤销教师 AI 权限")
    @DeleteMapping("/permissions/{teacherId}")
    public Result<Void> revokePermission(
            @RequestHeader("X-User-Role") Integer role,
            @PathVariable Long teacherId) {
        checkAdmin(role);
        aiPermissionService.revoke(teacherId);
        return Result.success();
    }

    @Operation(summary = "分页查询已授权教师列表")
    @GetMapping("/permissions")
    public Result<Page<AiPermissionVO>> listPermissions(
            @RequestHeader("X-User-Role") Integer role,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        checkAdmin(role);
        return Result.success(aiPermissionService.list(page, size));
    }

    @Operation(summary = "按工号搜索教师（含 AI 授权状态）")
    @GetMapping("/teachers/search")
    public Result<List<AiTeacherSearchVO>> searchTeachers(
            @RequestHeader("X-User-Role") Integer role,
            @RequestParam String keyword) {
        checkAdmin(role);
        Result<List<TeacherSearchVO>> feignResult = userFeignClient.searchTeachersByUserNo(keyword);
        List<TeacherSearchVO> teacherList = feignResult.getData();
        if (teacherList == null || teacherList.isEmpty()) {
            return Result.success(List.of());
        }
        List<AiTeacherSearchVO> voList = teacherList.stream().map(t -> {
            AiTeacherSearchVO vo = new AiTeacherSearchVO();
            vo.setId(t.getId());
            vo.setUserNo(t.getUserNo());
            vo.setRealName(t.getRealName());
            vo.setGranted(aiPermissionService.hasPermission(t.getId()));
            return vo;
        }).collect(Collectors.toList());
        return Result.success(voList);
    }
}
