package com.bucher.gateway.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 全局异常处理器
 */
@Slf4j
@Order(-1)
@Component
@RequiredArgsConstructor
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        // 设置响应类型
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // 确定状态码和消息
        int code;
        String message;

        if (ex instanceof ResponseStatusException rse) {
            code = rse.getStatusCode().value();
            message = rse.getReason();
        } else {
            code = HttpStatus.INTERNAL_SERVER_ERROR.value();
            message = "服务器内部错误";
            log.error("网关异常", ex);
        }

        // 构建响应体
        String body;
        try {
            body = objectMapper.writeValueAsString(new ErrorResponse(code, message, System.currentTimeMillis()));
        } catch (JsonProcessingException e) {
            body = String.format("{\"code\":%d,\"message\":\"%s\",\"timestamp\":%d}",
                    code, message, System.currentTimeMillis());
        }

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * 错误响应对象
     */
    public record ErrorResponse(int code, String message, long timestamp) {}
}
