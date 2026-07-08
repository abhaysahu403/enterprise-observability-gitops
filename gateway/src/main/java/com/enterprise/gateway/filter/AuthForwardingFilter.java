package com.enterprise.gateway.filter;

import com.enterprise.gateway.config.GatewayJwtService;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Central gateway filter that:
 *  1. Assigns/propagates a correlation request id (X-Request-Id) to every request.
 *  2. Validates the JWT on protected routes and rejects invalid/expired tokens
 *     before they ever reach a downstream service.
 *  3. Forwards resolved identity (username + roles) to downstream services via
 *     trusted headers, so those services don't need to re-parse the JWT.
 *  4. Logs a single structured summary line per request (central logging).
 */
@Component
public class AuthForwardingFilter implements WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger("com.enterprise.gateway.access");

    // Routes that do not require a valid JWT
    private static final Set<String> PUBLIC_PREFIXES = Set.of(
            "/api/auth/login", "/api/auth/register", "/api/auth/refresh",
            "/actuator", "/fallback"
    );

    private final GatewayJwtService jwtService;

    public AuthForwardingFilter(GatewayJwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        long start = System.currentTimeMillis();

        String requestId = Optional.ofNullable(request.getHeaders().getFirst("X-Request-Id"))
                .orElse(UUID.randomUUID().toString());

        ServerHttpRequest.Builder mutatedRequest = request.mutate().header("X-Request-Id", requestId);

        boolean isPublic = PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);

        if (!isPublic) {
            String authHeader = request.getHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return reject(exchange, requestId, "Missing bearer token");
            }
            String token = authHeader.substring(7);
            Optional<Claims> claims = jwtService.validate(token);
            if (claims.isEmpty()) {
                return reject(exchange, requestId, "Invalid or expired token");
            }
            List<String> roles = jwtService.extractRoles(claims.get());
            mutatedRequest.header("X-Auth-User", claims.get().getSubject())
                    .header("X-Auth-Roles", String.join(",", roles));
        }

        ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest.build()).build();
        mutatedExchange.getResponse().getHeaders().add("X-Request-Id", requestId);

        return chain.filter(mutatedExchange)
                .doFinally(signal -> {
                    long duration = System.currentTimeMillis() - start;
                    int status = mutatedExchange.getResponse().getStatusCode() != null
                            ? mutatedExchange.getResponse().getStatusCode().value() : 0;
                    log.info("requestId={} method={} path={} status={} durationMs={}",
                            requestId, request.getMethod(), path, status, duration);
                });
    }

    private Mono<Void> reject(ServerWebExchange exchange, String requestId, String reason) {
        log.warn("requestId={} rejected: {}", requestId, reason);
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        response.getHeaders().add("X-Request-Id", requestId);
        DataBufferFactory bufferFactory = response.bufferFactory();
        String body = String.format(
                "{\"success\":false,\"message\":\"%s\",\"requestId\":\"%s\"}", reason, requestId);
        DataBuffer buffer = bufferFactory.wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
