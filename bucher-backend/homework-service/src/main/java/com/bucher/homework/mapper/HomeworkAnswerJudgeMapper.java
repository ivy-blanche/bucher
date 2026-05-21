package com.bucher.homework.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bucher.homework.entity.HomeworkAnswerJudge;
import org.apache.ibatis.annotations.Mapper;

/**
 * 编程题判题结果 Mapper
 */
@Mapper
public interface HomeworkAnswerJudgeMapper extends BaseMapper<HomeworkAnswerJudge> {
}
