package com.bucher.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bucher.ai.config.AiModelProvider;
import com.bucher.ai.entity.AiConversation;
import com.bucher.ai.entity.AiCourseConfig;
import com.bucher.ai.mapper.AiConversationMapper;
import com.bucher.ai.mapper.AiCourseConfigMapper;
import com.bucher.ai.service.ChatService;
import com.bucher.ai.vo.ChatMessageVO;
import com.bucher.common.enums.ResultCodeEnum;
import com.bucher.common.exception.BusinessException;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

/**
 * RAG 对话服务实现 — 流式 SSE 输出
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final int MAX_HISTORY_ROUNDS = 20;
    private static final int MAX_RETRIEVAL_RESULTS = 8;
    private static final double MIN_SCORE = 0.7;

    private final AiModelProvider modelProvider;
    private final PgVectorEmbeddingStore embeddingStore;
    private final AiConversationMapper conversationMapper;
    private final AiCourseConfigMapper courseConfigMapper;

    @Override
    public Flux<String> streamChat(Long courseId, Long userId, Integer role, String message) {
        // 校验课程 AI 是否启用
        checkCourseAiEnabled(courseId);

        // 保存用户消息
        saveConversation(courseId, userId, "USER", message);

        // 加载历史消息 + 检索文档 + 流式调用 LLM
        return Flux.defer(() -> {
            try {
                List<ChatMessage> history = loadHistory(courseId, userId);
                String context = retrieveContext(courseId, message);
                List<ChatMessage> messages = buildMessages(context, history, message);

                StreamingChatLanguageModel model = modelProvider.getStreamingChatModel();
                StringBuilder fullResponse = new StringBuilder();

                return Flux.<String>create(sink -> {
                    model.chat(messages, new StreamingChatResponseHandler() {
                        @Override
                        public void onPartialResponse(String token) {
                            fullResponse.append(token);
                            sink.next(token);
                        }

                        @Override
                        public void onCompleteResponse(ChatResponse response) {
                            saveConversation(courseId, userId, "ASSISTANT", fullResponse.toString());
                            sink.complete();
                        }

                        @Override
                        public void onError(Throwable error) {
                            log.error("流式对话异常：courseId={}, userId={}", courseId, userId, error);
                            sink.error(error);
                        }
                    });
                }, FluxSink.OverflowStrategy.BUFFER);

            } catch (Exception e) {
                return Flux.error(e);
            }
        });
    }

    @Override
    public List<ChatMessageVO> getHistory(Long courseId, Long userId) {
        List<AiConversation> list = conversationMapper.selectList(
                new LambdaQueryWrapper<AiConversation>()
                        .eq(AiConversation::getCourseId, courseId)
                        .eq(AiConversation::getUserId, userId)
                        .eq(AiConversation::getIsDeleted, 0)
                        .orderByAsc(AiConversation::getCreateTime)
                        .last("LIMIT 50")
        );
        return list.stream().map(this::toChatMessageVo).collect(Collectors.toList());
    }

    private void checkCourseAiEnabled(Long courseId) {
        AiCourseConfig config = courseConfigMapper.selectOne(
                new LambdaQueryWrapper<AiCourseConfig>()
                        .eq(AiCourseConfig::getCourseId, courseId)
                        .eq(AiCourseConfig::getIsDeleted, 0)
                        .last("LIMIT 1"));
        if (config == null || !Integer.valueOf(1).equals(config.getStatus())) {
            throw new BusinessException(ResultCodeEnum.AI_COURSE_NOT_ENABLED);
        }
    }

    private void saveConversation(Long courseId, Long userId, String role, String content) {
        AiConversation conversation = new AiConversation();
        conversation.setCourseId(courseId);
        conversation.setUserId(userId);
        conversation.setRole(role);
        conversation.setContent(content);
        conversationMapper.insert(conversation);
    }

    private List<ChatMessage> loadHistory(Long courseId, Long userId) {
        List<AiConversation> records = conversationMapper.selectList(
                new LambdaQueryWrapper<AiConversation>()
                        .eq(AiConversation::getCourseId, courseId)
                        .eq(AiConversation::getUserId, userId)
                        .eq(AiConversation::getIsDeleted, 0)
                        .orderByDesc(AiConversation::getCreateTime)
                        .last("LIMIT " + MAX_HISTORY_ROUNDS)
        );

        List<ChatMessage> messages = new ArrayList<>();
        for (int i = records.size() - 1; i >= 0; i--) {
            AiConversation record = records.get(i);
            if ("USER".equals(record.getRole())) {
                messages.add(new UserMessage(record.getContent()));
            } else if ("ASSISTANT".equals(record.getRole())) {
                messages.add(new AiMessage(record.getContent()));
            }
        }
        return messages;
    }

    private String retrieveContext(Long courseId, String question) {
        EmbeddingModel embeddingModel = modelProvider.getEmbeddingModel();
        Embedding queryEmbedding = embeddingModel.embed(question).content();

        // 检索课程文档 AND 全局文档（course_id == 0）
        var filter = metadataKey("course_id").isEqualTo(courseId.toString())
                .or(metadataKey("course_id").isEqualTo("0"));

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(MAX_RETRIEVAL_RESULTS)
                .minScore(MIN_SCORE)
                .filter(filter)
                .build();

        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(request).matches();
        if (matches.isEmpty()) {
            log.debug("未检索到相关文档：courseId={}, question={}", courseId, question);
            return "";
        }

        return matches.stream()
                .map(m -> {
                    String source = m.embedded() != null
                            ? m.embedded().metadata().getString("file_name") : "";
                    String text = m.embedded() != null ? m.embedded().text() : "";
                    return "【来源：" + source + "】\n" + text;
                })
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    private List<ChatMessage> buildMessages(String context, List<ChatMessage> history, String question) {
        List<ChatMessage> messages = new ArrayList<>();
        StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append("你是一个智能学习助手。请根据以下参考资料回答用户的问题。");
        systemPrompt.append("\n如果参考资料中没有相关信息，请如实告知，不要编造。");
        systemPrompt.append("\n回答时请基于中文，简洁清晰。\n\n");

        if (!context.isEmpty()) {
            systemPrompt.append("参考资料：\n").append(context);
        }

        messages.add(new SystemMessage(systemPrompt.toString()));
        messages.addAll(history);
        messages.add(new UserMessage(question));
        return messages;
    }

    private ChatMessageVO toChatMessageVo(AiConversation record) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setRole(record.getRole());
        vo.setContent(record.getContent());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }
}
