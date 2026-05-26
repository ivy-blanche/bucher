package com.bucher.ai.controller;

import com.bucher.ai.dto.ChatRequestDTO;
import com.bucher.ai.enums.AiUserRoleEnum;
import com.bucher.ai.service.ChatService;
import com.bucher.ai.vo.ChatMessageVO;
import com.bucher.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AI 对话接口 — SSE 流式 RAG 对话
 */
@Tag(name = "AI 对话")
@RestController
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "流式对话（SSE）")
    @PostMapping(value = "/courses/{courseId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(
            @RequestHeader("X-User-Role") Integer role,
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long courseId,
            @Valid @RequestBody ChatRequestDTO dto) {
        return chatService.streamChat(courseId, userId, role, dto.getMessage());
    }

    @Operation(summary = "获取对话历史")
    @GetMapping("/courses/{courseId}/history")
    public Result<List<ChatMessageVO>> getHistory(
            @RequestHeader("X-User-Role") Integer role,
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long courseId) {
        return Result.success(chatService.getHistory(courseId, userId));
    }
}
