package com.bucher.ai.config;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bucher.ai.entity.AiConfig;
import com.bucher.ai.mapper.AiConfigMapper;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * AI 模型提供者 — 从 MySQL ai_config 动态构建模型实例。
 * <p>
 * 当管理员修改 AI 配置后，下次调用时自动重建模型。
 * 所有厂商（DEEPSEEK/OPENAI/QIANFAN/TONGYI/ZHIPU）均使用 OpenAI 兼容接口。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class AiModelProvider {

    private final AiConfigMapper aiConfigMapper;

    private volatile StreamingChatLanguageModel streamingChatModel;
    private volatile EmbeddingModel embeddingModel;
    private volatile String lastConfigHash;

    /**
     * 获取流式聊天模型
     */
    public StreamingChatLanguageModel getStreamingChatModel() {
        refreshIfNeeded();
        return streamingChatModel;
    }

    /**
     * 获取嵌入模型
     */
    public EmbeddingModel getEmbeddingModel() {
        refreshIfNeeded();
        return embeddingModel;
    }

    private void refreshIfNeeded() {
        AiConfig config = aiConfigMapper.selectOne(
                Wrappers.<AiConfig>lambdaQuery().eq(AiConfig::getIsDeleted, 0).last("LIMIT 1"));
        if (config == null) {
            throw new IllegalStateException("AI 配置不存在，请管理员先配置 AI 厂商参数");
        }

        String hash = DigestUtil.sha256Hex(
                config.getApiKey() + "|" + config.getApiEndpoint() + "|"
                        + config.getModelName() + "|" + config.getEmbeddingModel() + "|"
                        + config.getEmbeddingApiKey() + "|" + config.getEmbeddingApiEndpoint() + "|"
                        + config.getMaxTokens() + "|" + config.getTemperature() + "|"
                        + config.getProvider());
        if (hash.equals(lastConfigHash)) {
            return;
        }

        synchronized (this) {
            // double-check
            String reHash = DigestUtil.sha256Hex(
                    config.getApiKey() + "|" + config.getApiEndpoint() + "|"
                            + config.getModelName() + "|" + config.getEmbeddingModel() + "|"
                            + config.getEmbeddingApiKey() + "|" + config.getEmbeddingApiEndpoint() + "|"
                            + config.getMaxTokens() + "|" + config.getTemperature() + "|"
                            + config.getProvider());
            if (reHash.equals(lastConfigHash)) {
                return;
            }

            this.streamingChatModel = OpenAiStreamingChatModel.builder()
                    .apiKey(config.getApiKey())
                    .baseUrl(config.getApiEndpoint())
                    .modelName(config.getModelName())
                    .maxTokens(config.getMaxTokens())
                    .temperature(config.getTemperature().doubleValue())
                    .build();

            // Embedding 可独立配置 API Key/Endpoint，留空则复用 chat 的
            String embKey = config.getEmbeddingApiKey();
            String embEndpoint = config.getEmbeddingApiEndpoint();
            if (embKey == null || embKey.isBlank()) embKey = config.getApiKey();
            if (embEndpoint == null || embEndpoint.isBlank()) embEndpoint = config.getApiEndpoint();
            this.embeddingModel = OpenAiEmbeddingModel.builder()
                    .apiKey(embKey)
                    .baseUrl(embEndpoint)
                    .modelName(config.getEmbeddingModel())
                    .dimensions(1024) // text-embedding-v4 支持可配置维度，显式指定以对齐 pgvector
                    .maxSegmentsPerBatch(10) // DashScope 限制每批 ≤10
                    .build();

            this.lastConfigHash = reHash;
        }
    }
}
