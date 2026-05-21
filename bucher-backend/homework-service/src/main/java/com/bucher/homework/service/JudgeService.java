package com.bucher.homework.service;

import com.bucher.homework.dto.JudgeSubmitDTO;
import com.bucher.homework.vo.CodeRunResultVO;
import com.bucher.homework.vo.JudgeResultVO;

/**
 * 编程题判题服务
 */
public interface JudgeService {

    /**
     * 运行代码（仅样例测试用例，做作业时使用，不保存结果）
     */
    CodeRunResultVO runCode(Long studentId, JudgeSubmitDTO dto);

    /**
     * 异步判题（提交作业后调用，针对所有测试用例，保存结果）
     */
    void asyncJudgeProgrammingQuestions(Long studentId, Long homeworkId);

    /**
     * 查询判题状态
     */
    JudgeResultVO getJudgeStatus(Long answerId);
}
