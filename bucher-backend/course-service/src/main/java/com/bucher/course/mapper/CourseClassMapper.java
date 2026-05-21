package com.bucher.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bucher.course.entity.CourseClass;
import org.apache.ibatis.annotations.Mapper;

/**
 * 课程班级 Mapper
 */
@Mapper
public interface CourseClassMapper extends BaseMapper<CourseClass> {
}
