package com.bucher.course.service;

import com.bucher.course.dto.CourseCreateDTO;
import com.bucher.course.dto.CourseUpdateDTO;

import com.bucher.course.vo.CourseVO;
import com.bucher.course.vo.StudentCourseVO;
import com.bucher.course.vo.TeacherCourseListVO;

import java.util.List;

/**
 * 课程服务接口
 */
public interface CourseService {

    /**
     * 创建课程
     */
    CourseVO createCourse(CourseCreateDTO dto, Long teacherId, Integer role);

    /**
     * 更新课程
     */
    CourseVO updateCourse(CourseUpdateDTO dto, Long teacherId);

    /**
     * 删除课程（软删除）
     */
    void deleteCourse(Long courseId, Long teacherId);

    /**
     * 获取课程详情
     */
    CourseVO getCourseById(Long courseId);

    /**
     * 获取教师课程列表
     */
    List<TeacherCourseListVO> listTeacherCourses(Long teacherId);

    /**
     * 获取学生已选课程列表
     */
    List<StudentCourseVO> listStudentCourses(Long studentId);
}
