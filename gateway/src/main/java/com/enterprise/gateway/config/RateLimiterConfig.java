package com.enterprise.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * Basic rate limiting configuration. Requests are keyed by the caller's
 * remote address by default, falling back to the authenticated username
 * (forwarded via X-Auth-User) when present, so per-user limits apply
 * consistently even behind a shared NGINX/proxy IP.
 */
@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver rateLimitKeyResolver() {
        return exchange -> {
            String user = exchange.getRequest().getHeaders().getFirst("X-Auth-User");
            if (user != null) {
                return Mono.just(user);
            }
            String ip = Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                    .map(addr -> addr.getAddress().getHostAddress())
                    .orElse("unknown");
            return Mono.just(ip);
        };
    }
}
