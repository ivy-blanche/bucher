package com.bucher.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bucher.exam.entity.ExamSubmission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 考试提交记录 Mapper
 */
@Mapper
public interface ExamSubmissionMapper extends BaseMapper<ExamSubmission> {
}
