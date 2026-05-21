package com.bucher.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bucher.exam.entity.Exam;
import org.apache.ibatis.annotations.Mapper;

/**
 * 考试 Mapper
 */
@Mapper
public interface ExamMapper extends BaseMapper<Exam> {
}
