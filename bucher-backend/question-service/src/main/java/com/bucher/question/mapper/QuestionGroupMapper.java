package com.bucher.question.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bucher.question.entity.QuestionGroup;
import org.apache.ibatis.annotations.Mapper;

/**
 * 题库分组 Mapper
 */
@Mapper
public interface QuestionGroupMapper extends BaseMapper<QuestionGroup> {
}
