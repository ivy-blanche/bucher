package com.bucher.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bucher.course.entity.CourseEnrollment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 课程选课 Mapper
 */
@Mapper
public interface CourseEnrollmentMapper extends BaseMapper<CourseEnrollment> {
}
