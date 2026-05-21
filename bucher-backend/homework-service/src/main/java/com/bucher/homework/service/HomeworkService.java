package com.bucher.homework.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bucher.homework.dto.AnswerSaveDTO;
import com.bucher.homework.dto.HomeworkDraftSaveDTO;
import com.bucher.homework.dto.HomeworkListQueryDTO;
import com.bucher.homework.dto.HomeworkPublishDTO;
import com.bucher.homework.dto.HomeworkSubmitDTO;
import com.bucher.homework.vo.HomeworkDoVO;
import com.bucher.homework.vo.HomeworkListVO;
import com.bucher.homework.vo.HomeworkUnpublishedVO;
import com.bucher.homework.vo.StudentHomeworkVO;
import java.util.List;

/**
 * 作业 Service 接口
 */
public interface HomeworkService {

    /**
     * 分页查询教师作业列表
     */
    IPage<HomeworkListVO> getTeacherHomeworkList(HomeworkListQueryDTO dto);

    /**
     * 分页查询教师未发布作业列表
     */
    IPage<HomeworkUnpublishedVO> getUnpublishedHomeworkList(Long teacherId, Integer pageNum, Integer pageSize);

    /**
     * 保存作业草稿
     */
    void saveDraft(HomeworkDraftSaveDTO dto, Long teacherId, Integer role);

    /**
     * 发布作业
     */
    void publish(HomeworkPublishDTO dto, Long teacherId, Integer role);

    /**
     * 查询学生端作业列表
     */
    List<StudentHomeworkVO> getStudentHomeworkList(Long studentId, Long courseId);

    /**
     * 获取学生端做题页面数据（含已暂存/已提交答案）
     */
    HomeworkDoVO getHomeworkDetail(Long studentId, Long homeworkId);

    /**
     * 暂存单题答案到 Redis
     */
    void saveAnswer(Long studentId, AnswerSaveDTO dto);

    /**
     * 提交全部答案（Redis 刷入 MySQL）
     */
    void submitHomework(Long studentId, HomeworkSubmitDTO dto);
}
