package com.bucher.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bucher.exam.entity.ExamClass;
import org.apache.ibatis.annotations.Mapper;

/**
 * 考试教学班关联 Mapper
 */
@Mapper
public interface ExamClassMapper extends BaseMapper<ExamClass> {
}
