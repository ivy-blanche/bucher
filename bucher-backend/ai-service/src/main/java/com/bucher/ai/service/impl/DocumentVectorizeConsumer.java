package com.bucher.ai.service.impl;

import com.bucher.ai.config.AiModelProvider;
import com.bucher.ai.config.RabbitMQConfig;
import com.bucher.ai.entity.AiKnowledgeDocument;
import com.bucher.ai.mapper.AiKnowledgeDocumentMapper;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档向量化消费者 — 异步处理文档上传后的向量化流程
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentVectorizeConsumer {

    private final AiKnowledgeDocumentMapper documentMapper;
    private final MinioClient minioClient;
    private final PgVectorEmbeddingStore embeddingStore;
    private final AiModelProvider modelProvider;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_DOCUMENT_VECTORIZE)
    public void handleVectorize(String documentIdStr) {
        Long documentId = Long.parseLong(documentIdStr);
        log.info("开始向量化文档：documentId={}", documentId);

        AiKnowledgeDocument doc = documentMapper.selectById(documentId);
        if (doc == null) {
            log.warn("文档不存在：documentId={}", documentId);
            return;
        }

        Path tempFile = null;
        try {
            // 更新状态为处理中
            doc.setStatus(1);
            documentMapper.updateById(doc);

            // 从 MinIO 下载文件到临时目录
            tempFile = Files.createTempFile("vectorize_", "_" + doc.getFileName());
            try (InputStream is = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(doc.getBucketName())
                    .object(doc.getObjectName())
                    .build())) {
                Files.copy(is, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            // Apache Tika 解析文档
            Document parsedDoc;
            try (InputStream docIs = Files.newInputStream(tempFile)) {
                parsedDoc = new ApacheTikaDocumentParser().parse(docIs);
            }

            // 向文档元数据中注入 course_id 和 document_id（会沿用到分块后的 TextSegment）
            parsedDoc.metadata().put("file_name", doc.getFileName());
            parsedDoc.metadata().put("course_id", String.valueOf(doc.getCourseId()));
            parsedDoc.metadata().put("document_id", String.valueOf(doc.getId()));

            // 分块
            List<TextSegment> segments = DocumentSplitters.recursive(500, 100).split(parsedDoc);

            // 批量向量化
            EmbeddingModel embeddingModel = modelProvider.getEmbeddingModel();
            List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

            // 存储到 PGVector
            List<String> ids = embeddingStore.addAll(embeddings, segments);

            // 更新文档记录
            doc.setStatus(1); // 已向量化
            doc.setChunkCount(segments.size());
            documentMapper.updateById(doc);

            log.info("文档向量化完成：documentId={}, chunks={}, embeddingIds={}",
                    documentId, segments.size(), ids.size());

        } catch (Exception e) {
            log.error("文档向量化失败：documentId={}", documentId, e);
            doc.setStatus(2); // 处理失败
            doc.setErrorMsg(e.getMessage() != null ? e.getMessage().substring(0,
                    Math.min(e.getMessage().length(), 500)) : "未知错误");
            documentMapper.updateById(doc);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Exception e) {
                    log.warn("临时文件清理失败：{}", tempFile, e);
                }
            }
        }
    }

    @RabbitListener(queues = RabbitMQConfig.DLQ)
    public void handleVectorizeDlq(String documentIdStr) {
        log.warn("文档向量化进入死信队列：documentId={}", documentIdStr);
    }
}
