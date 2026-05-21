package com.bucher.question.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bucher.question.dto.QuestionGroupCreateDTO;
import com.bucher.question.dto.QuestionGroupUpdateDTO;
import com.bucher.question.entity.QuestionGroup;
import com.bucher.question.mapper.QuestionGroupMapper;
import com.bucher.question.service.QuestionGroupService;
import com.bucher.question.vo.QuestionGroupVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 题库分组 Service 实现
 */
@Service
@RequiredArgsConstructor
public class QuestionGroupServiceImpl implements QuestionGroupService {

    private final QuestionGroupMapper questionGroupMapper;

    @Override
    public List<QuestionGroupVO> listByTeacherId(Long teacherId) {
        LambdaQueryWrapper<QuestionGroup> wrapper = new LambdaQueryWrapper<QuestionGroup>()
                .eq(QuestionGroup::getTeacherId, teacherId)
                .orderByAsc(QuestionGroup::getSortOrder)
                .orderByDesc(QuestionGroup::getCreateTime);

        List<QuestionGroup> groups = questionGroupMapper.selectList(wrapper);

        return groups.stream()
                .map(g -> QuestionGroupVO.builder()
                        .id(g.getId())
                        .name(g.getName())
                        .questionCount(g.getQuestionCount())
                        .createTime(g.getCreateTime())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public Long create(QuestionGroupCreateDTO dto, Long teacherId) {
        QuestionGroup group = new QuestionGroup();
        group.setTeacherId(teacherId);
        group.setName(dto.getName());
        group.setSortOrder(0);
        group.setQuestionCount(0);

        questionGroupMapper.insert(group);

        return group.getId();
    }

    @Override
    public void updateName(Long id, QuestionGroupUpdateDTO dto, Long teacherId) {
        QuestionGroup group = new QuestionGroup();
        group.setId(id);
        group.setName(dto.getName());

        LambdaQueryWrapper<QuestionGroup> wrapper = new LambdaQueryWrapper<QuestionGroup>()
                .eq(QuestionGroup::getId, id)
                .eq(QuestionGroup::getTeacherId, teacherId);

        questionGroupMapper.update(group, wrapper);
    }

    @Override
    public void delete(Long id, Long teacherId) {
        LambdaQueryWrapper<QuestionGroup> wrapper = new LambdaQueryWrapper<QuestionGroup>()
                .eq(QuestionGroup::getId, id)
                .eq(QuestionGroup::getTeacherId, teacherId);

        questionGroupMapper.delete(wrapper);
    }
}
