package com.bucher.ai.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bucher.ai.vo.KnowledgeDocumentVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识库服务接口
 */
public interface KnowledgeBaseService {

    /**
     * 上传文档到课程知识库
     *
     * @param courseId  课程ID
     * @param teacherId 教师ID
     * @param file      上传文件
     * @return 文档信息
     */
    KnowledgeDocumentVO upload(Long courseId, Long teacherId, MultipartFile file);

    /**
     * 分页查询课程文档列表
     */
    Page<KnowledgeDocumentVO> listByCourse(Long courseId, Long teacherId, Integer page, Integer size);

    /**
     * 分页查询全局文档列表（管理员）
     */
    Page<KnowledgeDocumentVO> listGlobal(Integer page, Integer size);

    /**
     * 删除文档
     */
    void delete(Long documentId, Long teacherId, boolean isAdmin);
}
