package com.bucher.course.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bucher.common.enums.ResultCodeEnum;
import com.bucher.common.exception.BusinessException;
import com.bucher.course.dto.CourseCreateDTO;
import com.bucher.course.dto.CourseUpdateDTO;
import com.bucher.course.entity.Course;
import com.bucher.course.entity.CourseClass;
import com.bucher.course.entity.CourseEnrollment;
import com.bucher.course.feign.UserFeignClient;
import com.bucher.course.mapper.CourseClassMapper;
import com.bucher.course.mapper.CourseEnrollmentMapper;
import com.bucher.course.mapper.CourseMapper;
import com.bucher.course.service.CourseService;
import com.bucher.course.vo.CourseVO;
import com.bucher.course.vo.StudentCourseVO;
import com.bucher.course.vo.TeacherCourseListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 课程服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseMapper courseMapper;
    private final CourseClassMapper courseClassMapper;
    private final CourseEnrollmentMapper courseEnrollmentMapper;
    private final UserFeignClient userFeignClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseVO createCourse(CourseCreateDTO dto, Long teacherId, Integer role) {
        if (!Integer.valueOf(2).equals(role)) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN_OPERATION);
        }

        Course course = new Course();
        course.setName(dto.getName());
        course.setTeacherId(teacherId);
        course.setSemester(dto.getSemester());
        course.setDescription(dto.getDescription());
        course.setStatus(1);
        courseMapper.insert(course);

        log.info("教师创建课程成功: teacherId={}, courseId={}, name={}", teacherId, course.getId(), course.getName());
        return toVO(course, 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseVO updateCourse(CourseUpdateDTO dto, Long teacherId) {
        Course course = courseMapper.selectById(dto.getId());
        if (course == null) {
            throw new BusinessException(ResultCodeEnum.COURSE_NOT_FOUND);
        }
        if (!course.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ResultCodeEnum.COURSE_CLASS_FORBIDDEN);
        }

        course.setName(dto.getName());
        course.setSemester(dto.getSemester());
        if (dto.getDescription() != null) {
            course.setDescription(dto.getDescription());
        }
        if (dto.getStatus() != null) {
            course.setStatus(dto.getStatus());
        }
        courseMapper.updateById(course);

        Long classCount = courseClassMapper.selectCount(
                new LambdaQueryWrapper<CourseClass>().eq(CourseClass::getCourseId, course.getId())
        );
        return toVO(course, classCount.intValue());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCourse(Long courseId, Long teacherId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ResultCodeEnum.COURSE_NOT_FOUND);
        }
        if (!course.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ResultCodeEnum.COURSE_CLASS_FORBIDDEN);
        }

        courseMapper.deleteById(courseId);
        log.info("教师删除课程: teacherId={}, courseId={}", teacherId, courseId);
    }

    @Override
    public CourseVO getCourseById(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ResultCodeEnum.COURSE_NOT_FOUND);
        }

        Long classCount = courseClassMapper.selectCount(
                new LambdaQueryWrapper<CourseClass>().eq(CourseClass::getCourseId, courseId)
        );
        return toVO(course, classCount.intValue());
    }

    @Override
    public List<TeacherCourseListVO> listTeacherCourses(Long teacherId) {
        List<Course> courses = courseMapper.selectList(
                new LambdaQueryWrapper<Course>()
                        .eq(Course::getTeacherId, teacherId)
                        .orderByDesc(Course::getCreateTime)
        );

        return courses.stream()
                .map(c -> TeacherCourseListVO.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .semester(c.getSemester())
                        .courseCode(c.getCourseCode())
                        .build())
                .toList();
    }

    @Override
    public List<StudentCourseVO> listStudentCourses(Long studentId) {
        List<CourseEnrollment> enrollments = courseEnrollmentMapper.selectList(
                new LambdaQueryWrapper<CourseEnrollment>()
                        .eq(CourseEnrollment::getStudentId, studentId)
                        .eq(CourseEnrollment::getStatus, 1)
        );

        if (enrollments.isEmpty()) {
            return List.of();
        }

        List<Long> courseIds = enrollments.stream().map(CourseEnrollment::getCourseId).toList();
        List<Course> courses = courseMapper.selectList(
                new LambdaQueryWrapper<Course>().in(Course::getId, courseIds)
        );

        return courses.stream().map(c -> {
            String teacherName = null;
            try {
                teacherName = userFeignClient.getUserNameById(c.getTeacherId()).getData();
            } catch (Exception e) {
                log.warn("获取教师姓名失败: teacherId={}", c.getTeacherId(), e);
            }
            return StudentCourseVO.builder()
                    .id(c.getId())
                    .name(c.getName())
                    .teacherId(c.getTeacherId())
                    .teacherName(teacherName)
                    .semester(c.getSemester())
                    .build();
        }).toList();
    }

    private CourseVO toVO(Course course, int classCount) {
        return CourseVO.builder()
                .id(course.getId())
                .name(course.getName())
                .teacherId(course.getTeacherId())
                .semester(course.getSemester())
                .description(course.getDescription())
                .coverUrl(course.getCoverUrl())
                .courseCode(course.getCourseCode())
                .status(course.getStatus())
                .classCount(classCount)
                .createTime(course.getCreateTime())
                .build();
    }
}
