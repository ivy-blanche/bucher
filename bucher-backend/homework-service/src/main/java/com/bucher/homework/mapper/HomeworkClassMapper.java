package com.bucher.homework.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bucher.homework.entity.HomeworkClass;
import org.apache.ibatis.annotations.Mapper;

/**
 * 作业教学班关联 Mapper
 */
@Mapper
public interface HomeworkClassMapper extends BaseMapper<HomeworkClass> {
}
