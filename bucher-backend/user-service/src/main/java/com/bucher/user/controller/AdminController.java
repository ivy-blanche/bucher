package com.bucher.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bucher.common.enums.ResultCodeEnum;
import com.bucher.common.exception.BusinessException;
import com.bucher.common.result.Result;
import com.bucher.user.dto.*;
import com.bucher.user.entity.Department;
import com.bucher.user.enums.UserRoleEnum;
import com.bucher.user.service.AdminService;
import com.bucher.user.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "管理员接口")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    private void checkAdmin(Integer role) {
        if (role == null || !UserRoleEnum.ADMIN.getCode().equals(role)) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN);
        }
    }

    // ═══════════════════════════════════════════
    // Department
    // ═══════════════════════════════════════════

    @Operation(summary = "分页查询院系列表")
    @GetMapping("/departments")
    public Result<Page<DepartmentPageVO>> listDepartments(
            @RequestHeader("X-User-Role") Integer role,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer type) {
        checkAdmin(role);
        return Result.success(adminService.listDepartments(page, size, name, type));
    }

    @Operation(summary = "获取院系详情")
    @GetMapping("/departments/{id}")
    public Result<Department> getDepartment(
            @RequestHeader("X-User-Role") Integer role,
            @PathVariable Long id) {
        checkAdmin(role);
        return Result.success(adminService.getDepartmentById(id));
    }

    @Operation(summary = "新增院系")
    @PostMapping("/departments")
    public Result<DepartmentVO> createDepartment(
            @RequestHeader("X-User-Role") Integer role,
            @Valid @RequestBody DepartmentCreateDTO dto) {
        checkAdmin(role);
        return Result.success(adminService.createDepartment(dto));
    }

    @Operation(summary = "修改院系")
    @PutMapping("/departments/{id}")
    public Result<DepartmentVO> updateDepartment(
            @RequestHeader("X-User-Role") Integer role,
            @PathVariable Long id,
            @Valid @RequestBody DepartmentCreateDTO dto) {
        checkAdmin(role);
        return Result.success(adminService.updateDepartment(id, dto));
    }

    @Operation(summary = "删除院系")
    @DeleteMapping("/departments/{id}")
    public Result<Void> deleteDepartment(
            @RequestHeader("X-User-Role") Integer role,
            @PathVariable Long id) {
        checkAdmin(role);
        adminService.deleteDepartment(id);
        return Result.success();
    }

    // ═══════════════════════════════════════════
    // AdminClass
    // ═══════════════════════════════════════════

    @Operation(summary = "分页查询行政班级列表")
    @GetMapping("/classes")
    public Result<Page<AdminClassVO>> listAdminClasses(
            @RequestHeader("X-User-Role") Integer role,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String name) {
        checkAdmin(role);
        return Result.success(adminService.listAdminClasses(page, size, deptId, name));
    }

    @Operation(summary = "获取行政班级详情")
    @GetMapping("/classes/{id}")
    public Result<AdminClassVO> getAdminClass(
            @RequestHeader("X-User-Role") Integer role,
            @PathVariable Long id) {
        checkAdmin(role);
        return Result.success(adminService.getAdminClassById(id));
    }

    @Operation(summary = "新增行政班级")
    @PostMapping("/classes")
    public Result<AdminClassVO> createAdminClass(
            @RequestHeader("X-User-Role") Integer role,
            @Valid @RequestBody AdminClassCreateDTO dto) {
        checkAdmin(role);
        return Result.success(adminService.createAdminClass(dto));
    }

    @Operation(summary = "修改行政班级")
    @PutMapping("/classes/{id}")
    public Result<AdminClassVO> updateAdminClass(
            @RequestHeader("X-User-Role") Integer role,
            @PathVariable Long id,
            @Valid @RequestBody AdminClassCreateDTO dto) {
        checkAdmin(role);
        return Result.success(adminService.updateAdminClass(id, dto));
    }

    @Operation(summary = "删除行政班级")
    @DeleteMapping("/classes/{id}")
    public Result<Void> deleteAdminClass(
            @RequestHeader("X-User-Role") Integer role,
            @PathVariable Long id) {
        checkAdmin(role);
        adminService.deleteAdminClass(id);
        return Result.success();
    }

    // ═══════════════════════════════════════════
    // Student Import
    // ═══════════════════════════════════════════

    @Operation(summary = "批量导入学生")
    @PostMapping("/students/import")
    public Result<ImportResultVO> importStudents(
            @RequestHeader("X-User-Role") Integer role,
            @RequestParam Long adminClassId,
            @RequestParam("file") MultipartFile file) {
        checkAdmin(role);
        return Result.success(adminService.importStudents(adminClassId, file));
    }

    // ═══════════════════════════════════════════
    // Teacher Import
    // ═══════════════════════════════════════════

    @Operation(summary = "批量导入教师")
    @PostMapping("/departments/{deptId}/teachers/import")
    public Result<ImportResultVO> importTeachers(
            @RequestHeader("X-User-Role") Integer role,
            @PathVariable Long deptId,
            @RequestParam("file") MultipartFile file) {
        checkAdmin(role);
        return Result.success(adminService.importTeachers(deptId, file));
    }

    // ═══════════════════════════════════════════
    // Password Reset
    // ═══════════════════════════════════════════

    @Operation(summary = "管理员重置用户密码")
    @PutMapping("/users/{id}/reset-password")
    public Result<Void> resetUserPassword(
            @RequestHeader("X-User-Role") Integer role,
            @RequestHeader("X-User-Id") Long operatorId,
            @PathVariable Long id) {
        checkAdmin(role);
        adminService.resetUserPassword(id, operatorId);
        return Result.success();
    }

    // ═══════════════════════════════════════════
    // User List
    // ═══════════════════════════════════════════

    @Operation(summary = "分页查询用户列表")
    @GetMapping("/users")
    public Result<Page<UserPageVO>> listUsers(
            @RequestHeader("X-User-Role") Integer role,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false, name = "userRole") Integer userRole,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Long adminClassId,
            @RequestParam(required = false) String keyword) {
        checkAdmin(role);
        return Result.success(adminService.listUsers(page, size, userRole, deptId, adminClassId, keyword));
    }
}
