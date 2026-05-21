package com.bucher.question.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bucher.question.entity.QuestionProgramming;
import org.apache.ibatis.annotations.Mapper;

/**
 * 编程题配置 Mapper
 */
@Mapper
public interface QuestionProgrammingMapper extends BaseMapper<QuestionProgramming> {
}
