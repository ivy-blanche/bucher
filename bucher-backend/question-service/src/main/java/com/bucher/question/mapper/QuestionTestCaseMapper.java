package com.bucher.question.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bucher.question.entity.QuestionTestCase;
import org.apache.ibatis.annotations.Mapper;

/**
 * 编程题测试用例 Mapper
 */
@Mapper
public interface QuestionTestCaseMapper extends BaseMapper<QuestionTestCase> {
}
