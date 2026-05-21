package com.bucher.course.controller;

import com.bucher.common.result.Result;
import com.bucher.course.dto.CourseCreateDTO;
import com.bucher.course.dto.CourseUpdateDTO;
import com.bucher.course.service.CourseService;
import com.bucher.course.vo.CourseVO;
import com.bucher.course.vo.StudentCourseVO;
import com.bucher.course.vo.TeacherCourseListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 课程控制器
 */
@Tag(name = "课程管理")
@RestController
@RequestMapping("/course")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @Operation(summary = "创建课程")
    @PostMapping
    public Result<CourseVO> create(@Valid @RequestBody CourseCreateDTO dto,
                                   @RequestHeader("X-User-Id") Long userId,
                                   @RequestHeader("X-User-Role") Integer role) {
        return Result.success(courseService.createCourse(dto, userId, role));
    }

    @Operation(summary = "获取教师课程列表")
    @GetMapping("/list")
    public Result<List<TeacherCourseListVO>> list(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(courseService.listTeacherCourses(userId));
    }

    @Operation(summary = "获取课程详情")
    @GetMapping("/{id}")
    public Result<CourseVO> getById(@PathVariable Long id) {
        return Result.success(courseService.getCourseById(id));
    }

    @Operation(summary = "更新课程")
    @PutMapping
    public Result<CourseVO> update(@Valid @RequestBody CourseUpdateDTO dto,
                                   @RequestHeader("X-User-Id") Long userId) {
        return Result.success(courseService.updateCourse(dto, userId));
    }

    @Operation(summary = "删除课程")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               @RequestHeader("X-User-Id") Long userId) {
        courseService.deleteCourse(id, userId);
        return Result.success();
    }

    @Operation(summary = "获取学生已选课程列表")
    @GetMapping("/student/list")
    public Result<List<StudentCourseVO>> listStudentCourses(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(courseService.listStudentCourses(userId));
    }
}
