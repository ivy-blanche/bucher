package com.bucher.question.service;

import com.bucher.question.dto.QuestionGroupCreateDTO;
import com.bucher.question.dto.QuestionGroupUpdateDTO;
import com.bucher.question.vo.QuestionGroupVO;

import java.util.List;

/**
 * 题库分组 Service
 */
public interface QuestionGroupService {

    /**
     * 查询教师名下的题库分组列表
     */
    List<QuestionGroupVO> listByTeacherId(Long teacherId);

    /**
     * 创建题库分组
     */
    Long create(QuestionGroupCreateDTO dto, Long teacherId);

    /**
     * 修改题库分组名称
     */
    void updateName(Long id, QuestionGroupUpdateDTO dto, Long teacherId);

    /**
     * 删除题库分组
     */
    void delete(Long id, Long teacherId);
}
