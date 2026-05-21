package com.bucher.homework.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bucher.homework.entity.HomeworkSubmission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 作业提交记录 Mapper
 */
@Mapper
public interface HomeworkSubmissionMapper extends BaseMapper<HomeworkSubmission> {
}
