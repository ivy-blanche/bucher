package com.bucher.homework.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bucher.homework.entity.HomeworkQuestion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 作业题目关联 Mapper
 */
@Mapper
public interface HomeworkQuestionMapper extends BaseMapper<HomeworkQuestion> {
}
