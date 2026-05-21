package com.bucher.question.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bucher.question.entity.QuestionOption;
import org.apache.ibatis.annotations.Mapper;

/**
 * 题目选项 Mapper
 */
@Mapper
public interface QuestionOptionMapper extends BaseMapper<QuestionOption> {
}
