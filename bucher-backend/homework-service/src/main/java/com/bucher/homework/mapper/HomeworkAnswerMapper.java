package com.bucher.homework.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bucher.homework.entity.HomeworkAnswer;
import org.apache.ibatis.annotations.Mapper;

/**
 * 作业作答记录 Mapper
 */
@Mapper
public interface HomeworkAnswerMapper extends BaseMapper<HomeworkAnswer> {
}
