package com.bucher.exam.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bucher.exam.dto.AnswerSaveDTO;
import com.bucher.exam.dto.ExamDraftSaveDTO;
import com.bucher.exam.dto.ExamListQueryDTO;
import com.bucher.exam.dto.ExamPublishDTO;
import com.bucher.exam.dto.ExamSubmitDTO;
import com.bucher.exam.vo.ExamDoVO;
import com.bucher.exam.vo.ExamListVO;
import com.bucher.exam.vo.ExamUnpublishedVO;
import com.bucher.exam.vo.StudentExamVO;

import java.util.List;

/**
 * 考试 Service 接口
 */
public interface ExamService {

    /**
     * 分页查询教师考试列表
     */
    IPage<ExamListVO> getTeacherExamList(ExamListQueryDTO dto);

    /**
     * 分页查询教师未发布考试列表
     */
    IPage<ExamUnpublishedVO> getUnpublishedExamList(Long teacherId, Integer pageNum, Integer pageSize);

    /**
     * 保存考试草稿
     */
    void saveDraft(ExamDraftSaveDTO dto, Long teacherId, Integer role);

    /**
     * 发布考试
     */
    void publish(ExamPublishDTO dto, Long teacherId, Integer role);

    /**
     * 查询学生端考试列表
     */
    List<StudentExamVO> getStudentExamList(Long studentId, Long courseId);

    /**
     * 获取学生端做题页面数据
     */
    ExamDoVO getExamDetail(Long studentId, Long examId);

    /**
     * 暂存单题答案到 Redis
     */
    void saveAnswer(Long studentId, AnswerSaveDTO dto);

    /**
     * 提交全部答案
     */
    void submitExam(Long studentId, ExamSubmitDTO dto);

    /**
     * 超时自动提交（定时任务或接口调用）
     */
    void autoSubmitExpiredExams();
}
