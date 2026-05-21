package com.bucher.question.service;

import com.bucher.question.dto.QuestionBatchSaveDTO;
import com.bucher.question.vo.QuestionDetailVO;
import com.bucher.question.vo.QuestionImportResultVO;
import com.bucher.question.vo.QuestionImportUploadVO;
import com.bucher.question.vo.QuestionListVO;

import java.io.OutputStream;
import java.util.List;

/**
 * 题目 Service
 */
public interface QuestionService {

    /**
     * 查询指定题库下的题目列表
     */
    List<QuestionListVO> listByGroupId(Long groupId);

    /**
     * 查询题目详情（含选项）
     */
    QuestionDetailVO getDetail(Long id);

    /**
     * 批量查询题目详情（含选项）
     */
    List<QuestionDetailVO> getBatchDetail(List<Long> ids);

    /**
     * 批量保存题目（新增/修改/删除）
     */
    void batchSave(QuestionBatchSaveDTO dto, Long teacherId);

    /**
     * 下载导入模板
     */
    void downloadTemplate(OutputStream outputStream);

    /**
     * 上传导入文件
     */
    QuestionImportUploadVO uploadFile(String originalName, java.io.InputStream inputStream);

    /**
     * 执行导入
     */
    QuestionImportResultVO executeImport(Long groupId, String fileKey, Long teacherId);
}
