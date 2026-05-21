package com.bucher.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bucher.exam.entity.ExamQuestion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 考试题目关联 Mapper
 */
@Mapper
public interface ExamQuestionMapper extends BaseMapper<ExamQuestion> {
}
