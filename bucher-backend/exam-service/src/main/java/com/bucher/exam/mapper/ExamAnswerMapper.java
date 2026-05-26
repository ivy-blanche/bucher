package com.bucher.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bucher.exam.entity.ExamAnswer;
import org.apache.ibatis.annotations.Mapper;

/**
 * 考试作答记录 Mapper
 */
@Mapper
public interface ExamAnswerMapper extends BaseMapper<ExamAnswer> {
}
