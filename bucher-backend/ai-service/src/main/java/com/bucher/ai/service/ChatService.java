package com.bucher.ai.service;

import com.bucher.ai.vo.ChatMessageVO;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AI 对话服务接口
 */
public interface ChatService {

    /**
     * 流式 RAG 对话（SSE）
     *
     * @param courseId 课程ID
     * @param userId   用户ID
     * @param role     用户角色：2=教师，3=学生
     * @param message  用户消息
     * @return SSE 流式响应
     */
    Flux<String> streamChat(Long courseId, Long userId, Integer role, String message);

    /**
     * 获取对话历史
     */
    List<ChatMessageVO> getHistory(Long courseId, Long userId);
}
