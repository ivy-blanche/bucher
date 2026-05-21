package com.bucher.course.controller;

import cn.hutool.core.util.StrUtil;
import com.bucher.common.result.Result;
import com.bucher.course.dto.CourseClassCreateDTO;
import com.bucher.course.dto.JoinByInviteCodeDTO;
import com.bucher.course.service.CourseClassService;
import com.bucher.course.vo.CourseClassMemberVO;
import com.bucher.course.vo.CourseClassVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 课程班级控制器
 */
@Tag(name = "课程班级管理")
@RestController
@RequestMapping("/course/class")
@RequiredArgsConstructor
public class CourseClassController {

    private final CourseClassService courseClassService;

    @Operation(summary = "创建课程班级")
    @PostMapping
    public Result<CourseClassVO> create(@Valid @RequestBody CourseClassCreateDTO dto,
                                        @RequestHeader("X-User-Id") Long userId) {
        return Result.success(courseClassService.createClass(dto, userId));
    }

    @Operation(summary = "获取课程下的班级列表")
    @GetMapping("/list/{courseId}")
    public Result<List<CourseClassVO>> listByCourse(@PathVariable Long courseId) {
        return Result.success(courseClassService.listCourseClasses(courseId));
    }

    @Operation(summary = "更新班级名称")
    @PutMapping("/{id}")
    public Result<Void> updateName(@PathVariable Long id,
                                   @Parameter(description = "班级名称") @RequestParam String name,
                                   @RequestHeader("X-User-Id") Long userId) {
        courseClassService.updateClassName(id, name, userId);
        return Result.success();
    }

    @Operation(summary = "删除班级")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               @RequestHeader("X-User-Id") Long userId) {
        courseClassService.deleteClass(id, userId);
        return Result.success();
    }

    @Operation(summary = "刷新邀请码")
    @PutMapping("/{id}/invite-code")
    public Result<String> regenerateInviteCode(@PathVariable Long id,
                                                @RequestHeader("X-User-Id") Long userId) {
        return Result.success(courseClassService.regenerateInviteCode(id, userId));
    }

    @Operation(summary = "学生通过邀请码加入班级")
    @PostMapping("/join")
    public Result<Void> joinByInviteCode(@Valid @RequestBody JoinByInviteCodeDTO dto,
                                          @RequestHeader("X-User-Id") Long userId) {
        courseClassService.joinByInviteCode(dto, userId);
        return Result.success();
    }

    @Operation(summary = "教师添加学生到班级")
    @PostMapping("/{id}/student/{studentId}")
    public Result<Void> addStudent(@PathVariable Long id,
                                   @PathVariable Long studentId,
                                   @RequestHeader("X-User-Id") Long userId) {
        courseClassService.addStudent(id, studentId, userId);
        return Result.success();
    }

    @Operation(summary = "从班级移除学生")
    @DeleteMapping("/member/{id}")
    public Result<Void> removeStudent(@PathVariable Long id,
                                      @RequestHeader("X-User-Id") Long userId) {
        courseClassService.removeStudent(id, userId);
        return Result.success();
    }

    @Operation(summary = "获取班级成员列表")
    @GetMapping("/{id}/members")
    public Result<List<CourseClassMemberVO>> listMembers(@PathVariable Long id) {
        return Result.success(courseClassService.listMembers(id));
    }
}
