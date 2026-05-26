package com.bucher.ai.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bucher.ai.config.RabbitMQConfig;
import com.bucher.ai.entity.AiKnowledgeDocument;
import com.bucher.ai.feign.CourseFeignClient;
import com.bucher.ai.feign.UserFeignClient;
import com.bucher.ai.mapper.AiKnowledgeDocumentMapper;
import com.bucher.ai.service.KnowledgeBaseService;
import com.bucher.ai.vo.KnowledgeDocumentVO;
import com.bucher.common.enums.ResultCodeEnum;
import com.bucher.common.exception.BusinessException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 知识库服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024L; // 20MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "docx", "pptx", "txt", "md", "html", "htm");

    private final AiKnowledgeDocumentMapper documentMapper;
    private final MinioClient minioClient;
    private final RabbitTemplate rabbitTemplate;
    private final CourseFeignClient courseFeignClient;
    private final UserFeignClient userFeignClient;

    @Value("${minio.bucket-name:ai-bucket}")
    private String bucketName;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocumentVO upload(Long courseId, Long teacherId, MultipartFile file) {
        // 校验文件
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.UPLOAD_FILE_EMPTY);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ResultCodeEnum.AI_UPLOAD_SIZE_EXCEEDED);
        }

        String fileName = file.getOriginalFilename();
        String ext = getExtension(fileName);
        if (StrUtil.isBlank(ext) || !ALLOWED_EXTENSIONS.contains(ext.toLowerCase())) {
            throw new BusinessException(ResultCodeEnum.KNOWLEDGE_FILE_TYPE_NOT_SUPPORTED);
        }

        // 校验课程存在
        if (courseId != 0) {
            String courseName = courseFeignClient.getCourseNameById(courseId).getData();
            if (StrUtil.isBlank(courseName)) {
                throw new BusinessException(ResultCodeEnum.COURSE_NOT_FOUND);
            }
        }

        // 生成雪花 ID（INSERT 前需要用）
        long docId = IdUtil.getSnowflakeNextId();

        // 保存到 MySQL
        AiKnowledgeDocument doc = new AiKnowledgeDocument();
        doc.setId(docId);
        doc.setCourseId(courseId);
        doc.setTeacherId(teacherId);
        doc.setFileName(fileName);
        doc.setFileSize(file.getSize());
        doc.setFileType(file.getContentType());
        doc.setFileExt(ext.toLowerCase());
        doc.setObjectName("ai/" + courseId + "/" + docId + "_" + fileName);
        doc.setBucketName(bucketName);
        doc.setStatus(0);
        doc.setIsDeleted(0);
        documentMapper.insert(doc);

        // 上传到 MinIO
        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(doc.getObjectName())
                    .stream(is, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception e) {
            log.error("MinIO 上传失败：{}", fileName, e);
            throw new BusinessException(ResultCodeEnum.MATERIAL_UPLOAD_FAILED);
        }

        // 事务提交后再发送 RabbitMQ 消息，防止消费者查不到未提交的数据
        final Long savedDocId = doc.getId();
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_AI,
                                RabbitMQConfig.ROUTING_KEY_VECTORIZE, savedDocId.toString());
                    }
                });

        log.info("文档上传成功：{}，documentId={}", fileName, doc.getId());
        return toVo(doc);
    }

    @Override
    public Page<KnowledgeDocumentVO> listByCourse(Long courseId, Long teacherId,
                                                   Integer page, Integer size) {
        LambdaQueryWrapper<AiKnowledgeDocument> wrapper = new LambdaQueryWrapper<AiKnowledgeDocument>()
                .eq(AiKnowledgeDocument::getCourseId, courseId)
                .orderByDesc(AiKnowledgeDocument::getCreateTime);

        Page<AiKnowledgeDocument> docPage = documentMapper.selectPage(
                new Page<>(page, size), wrapper);

        Page<KnowledgeDocumentVO> voPage = new Page<>(docPage.getCurrent(),
                docPage.getSize(), docPage.getTotal());
        voPage.setRecords(docPage.getRecords().stream()
                .map(this::toVo)
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public Page<KnowledgeDocumentVO> listGlobal(Integer page, Integer size) {
        return listByCourse(0L, null, page, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long documentId, Long teacherId, boolean isAdmin) {
        AiKnowledgeDocument doc = documentMapper.selectById(documentId);
        if (doc == null) {
            throw new BusinessException(ResultCodeEnum.KNOWLEDGE_DOCUMENT_NOT_FOUND);
        }
        // 校验权限：管理员可删任意文档，教师只可删自己的课程文档
        if (!isAdmin && !doc.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN_OPERATION);
        }

        documentMapper.deleteById(documentId);
        // 异步删除 MinIO 文件和 PGVector 向量
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_AI,
                "ai.document.delete", documentId.toString());

        log.info("文档已删除：documentId={}", documentId);
    }

    private KnowledgeDocumentVO toVo(AiKnowledgeDocument doc) {
        KnowledgeDocumentVO vo = new KnowledgeDocumentVO();
        BeanUtils.copyProperties(doc, vo);
        // 查询教师姓名
        try {
            var result = userFeignClient.getUserNameById(doc.getTeacherId());
            vo.setTeacherName(result.getData());
        } catch (Exception e) {
            log.warn("获取教师姓名失败：teacherId={}", doc.getTeacherId());
        }
        return vo;
    }

    private String getExtension(String fileName) {
        if (StrUtil.isBlank(fileName)) return null;
        int idx = fileName.lastIndexOf('.');
        return idx > 0 ? fileName.substring(idx + 1) : null;
    }
}
