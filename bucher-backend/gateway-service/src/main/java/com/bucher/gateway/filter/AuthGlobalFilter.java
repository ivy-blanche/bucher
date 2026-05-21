package com.bucher.gateway.filter;

import cn.hutool.core.util.StrUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT 认证过滤器
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${jwt.header:Authorization}")
    private String tokenHeader;

    @Value("${jwt.prefix:Bearer }")
    private String tokenPrefix;

    @Value("${jwt.white-list:}")
    private List<String> whiteList;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @PostConstruct
    public void init() {
        log.info("AuthGlobalFilter 白名单: {}", whiteList);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 白名单放行
        if (isWhitePath(path)) {
            log.debug("白名单放行: {}", path);
            return chain.filter(exchange);
        }

        // 获取 Token
        String token = getToken(request);
        log.debug("请求路径: {}, Token: {}", path, token != null ? "存在, 长度=" + token.length() : "不存在");
        if (StrUtil.isBlank(token)) {
            log.debug("Authorization header: {}", request.getHeaders().getFirst(tokenHeader));
            return unauthorized(exchange, "未登录或登录已过期");
        }

        try {
            // 解析 Token
            Claims claims = parseToken(token);

            // 将用户信息添加到请求头，传递给下游服务
            // userId 在 JWT 中存储为 Long，需先取 Object 再转 String
            Object userIdObj = claims.get("userId");
            String userId = userIdObj != null ? String.valueOf(userIdObj) : "";
            String userNo = claims.get("userNo", String.class);
            // role 在 JWT 中存储为 Integer，需要先取出再转为 String
            Object roleObj = claims.get("role");
            String role = roleObj != null ? String.valueOf(roleObj) : "";

            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId != null ? userId : "")
                    .header("X-User-No", userNo != null ? userNo : "")
                    .header("X-User-Role", role != null ? role : "")
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (ExpiredJwtException e) {
            log.warn("Token 已过期: {}", e.getMessage());
            return unauthorized(exchange, "登录已过期，请重新登录");
        } catch (Exception e) {
            log.error("Token 解析失败: {}", e.getMessage());
            return unauthorized(exchange, "无效的登录凭证");
        }
    }

    /**
     * 判断是否白名单路径
     */
    private boolean isWhitePath(String path) {
        if (whiteList == null || whiteList.isEmpty()) {
            log.debug("白名单为空或 null, path: {}", path);
            return false;
        }
        boolean matched = whiteList.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
        if (!matched) {
            log.debug("白名单未匹配, path: {}, patterns: {}", path, whiteList);
        }
        return matched;
    }

    /**
     * 从请求头获取 Token
     */
    private String getToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(tokenHeader);
        if (StrUtil.isBlank(header)) {
            return null;
        }
        if (header.startsWith(tokenPrefix)) {
            return header.substring(tokenPrefix.length());
        }
        return header;
    }

    /**
     * 解析 Token
     */
    private Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 返回未授权响应
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format("{\"code\":401,\"message\":\"%s\",\"data\":null,\"timestamp\":%d}",
                message, System.currentTimeMillis());
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
