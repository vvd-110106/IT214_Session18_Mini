package com.rikkeibank.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkeibank.gateway.util.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtils jwtUtils;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final List<String> WHITELISTED_PATHS = List.of(
            "/api/v1/auth/**",
            "/eureka/**",
            "/actuator/**"
    );

    @Autowired
    public JwtAuthenticationFilter(JwtUtils jwtUtils, @Autowired(required = false) ReactiveStringRedisTemplate redisTemplate) {
        this.jwtUtils = jwtUtils;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Check if path is whitelisted
        for (String pattern : WHITELISTED_PATHS) {
            if (pathMatcher.match(pattern, path)) {
                return chain.filter(exchange);
            }
        }

        // Get Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        if (!jwtUtils.validateToken(token)) {
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired JWT token");
        }

        try {
            Claims claims = jwtUtils.getClaimsFromToken(token);
            String username = claims.getSubject();
            String role = claims.get("role", String.class);
            String cif = claims.get("cif", String.class);
            String jti = claims.getId();

            // Check Redis blacklist (Admin force logout or user logout)
            if (redisTemplate != null) {
                String blacklistUserKey = "blacklist:user:" + username;
                String blacklistTokenKey = jti != null ? "blacklist:token:" + jti : null;

                return redisTemplate.hasKey(blacklistUserKey)
                        .flatMap(isUserBlacklisted -> {
                            if (Boolean.TRUE.equals(isUserBlacklisted)) {
                                return onError(exchange, HttpStatus.UNAUTHORIZED,
                                        "Tài khoản đã bị quản trị viên thu hồi quyền truy cập (Force Logout). Vui lòng đăng nhập lại.");
                            }
                            if (blacklistTokenKey != null) {
                                return redisTemplate.hasKey(blacklistTokenKey)
                                        .flatMap(isTokenBlacklisted -> {
                                            if (Boolean.TRUE.equals(isTokenBlacklisted)) {
                                                return onError(exchange, HttpStatus.UNAUTHORIZED,
                                                        "Phiên đăng nhập đã bị thu hồi.");
                                            }
                                            return forwardWithHeaders(exchange, chain, username, role, cif);
                                        });
                            }
                            return forwardWithHeaders(exchange, chain, username, role, cif);
                        })
                        .onErrorResume(e -> {
                            log.warn("Redis check failed, allowing proceed if token valid: {}", e.getMessage());
                            return forwardWithHeaders(exchange, chain, username, role, cif);
                        });
            }

            return forwardWithHeaders(exchange, chain, username, role, cif);

        } catch (Exception e) {
            log.error("Token parsing error: {}", e.getMessage());
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Failed to parse JWT token");
        }
    }

    private Mono<Void> forwardWithHeaders(ServerWebExchange exchange, GatewayFilterChain chain,
                                          String username, String role, String cif) {
        ServerHttpRequest.Builder builder = exchange.getRequest().mutate()
                .header("X-User-Name", username != null ? username : "")
                .header("X-User-Role", role != null ? role : "");

        if (cif != null) {
            builder.header("X-Customer-Cif", cif);
        }

        return chain.filter(exchange.mutate().request(builder.build()).build());
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", Instant.now().toString());
        errorDetails.put("status", status.value());
        errorDetails.put("error", status.getReasonPhrase());
        errorDetails.put("message", message);
        errorDetails.put("path", exchange.getRequest().getURI().getPath());

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(errorDetails);
        } catch (JsonProcessingException e) {
            bytes = ("{\"status\":" + status.value() + ",\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
