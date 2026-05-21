package com.bucher.course.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bucher.common.result.Result;
import com.bucher.course.entity.Course;
import com.bucher.course.entity.CourseClass;
import com.bucher.course.entity.CourseClassMember;
import com.bucher.course.mapper.CourseClassMapper;
import com.bucher.course.mapper.CourseClassMemberMapper;
import com.bucher.course.mapper.CourseMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 内部服务间调用接口（不经过网关鉴权）
 */
@Tag(name = "内部服务调用")
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalCourseController {

    private final CourseMapper courseMapper;
    private final CourseClassMemberMapper courseClassMemberMapper;
    private final CourseClassMapper courseClassMapper;

    @Operation(summary = "根据ID获取课程名称（内部Feign调用）")
    @GetMapping("/courses/{id}/name")
    public Result<String> getCourseNameById(@PathVariable Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            return Result.success(null);
        }
        return Result.success(course.getName());
    }

    @Operation(summary = "根据课程班级ID列表获取所有成员学生ID（内部Feign调用）")
    @GetMapping("/course-classes/members")
    public Result<List<Long>> getClassMemberIds(@RequestParam List<Long> classIds) {
        if (classIds == null || classIds.isEmpty()) {
            return Result.success(List.of());
        }
        LambdaQueryWrapper<CourseClassMember> wrapper = Wrappers.lambdaQuery(CourseClassMember.class)
                .in(CourseClassMember::getCourseClassId, classIds)
                .eq(CourseClassMember::getStatus, 1);
        List<CourseClassMember> members = courseClassMemberMapper.selectList(wrapper);
        List<Long> studentIds = members.stream()
                .map(CourseClassMember::getStudentId)
                .distinct()
                .collect(Collectors.toList());
        return Result.success(studentIds);
    }

    @Operation(summary = "获取学生在指定课程下的教学班ID列表（内部Feign调用）")
    @GetMapping("/course-classes/student-classes")
    public Result<List<Long>> getStudentCourseClassIds(
            @RequestParam("studentId") Long studentId,
            @RequestParam("courseId") Long courseId) {
        // 查询学生所属的所有教学班
        LambdaQueryWrapper<CourseClassMember> memberWrapper = Wrappers.lambdaQuery(CourseClassMember.class)
                .eq(CourseClassMember::getStudentId, studentId)
                .eq(CourseClassMember::getStatus, 1);
        List<CourseClassMember> members = courseClassMemberMapper.selectList(memberWrapper);
        if (members.isEmpty()) {
            return Result.success(List.of());
        }

        Set<Long> classIds = members.stream()
                .map(CourseClassMember::getCourseClassId)
                .collect(Collectors.toSet());

        // 过滤出属于指定课程的教学班
        LambdaQueryWrapper<CourseClass> classWrapper = Wrappers.lambdaQuery(CourseClass.class)
                .in(CourseClass::getId, classIds)
                .eq(CourseClass::getCourseId, courseId);
        List<CourseClass> classes = courseClassMapper.selectList(classWrapper);

        List<Long> result = classes.stream()
                .map(CourseClass::getId)
                .collect(Collectors.toList());
        return Result.success(result);
    }
}
