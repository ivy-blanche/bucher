package com.bucher.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bucher.course.entity.CourseClassMember;
import org.apache.ibatis.annotations.Mapper;

/**
 * 课程班级成员 Mapper
 */
@Mapper
public interface CourseClassMemberMapper extends BaseMapper<CourseClassMember> {
}
