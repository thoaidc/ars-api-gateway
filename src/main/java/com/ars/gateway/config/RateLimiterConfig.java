package com.ars.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Configuration
public class RateLimiterConfig {

    @Bean("compositeKeyResolver")
    public KeyResolver compositeKeyResolver() {
        return exchange -> Mono.just(resolveKey(exchange));
    }

    private String resolveKey(ServerWebExchange exchange) {
        // 1. Prioritize processing Authorization tokens first
        ServerHttpRequest request = exchange.getRequest();
        String authHeader = request.getHeaders().getFirst("Authorization");

        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (!token.isBlank())
                return "token:" + token;
        }

        // 2. Fallback by real IP (check X-Forwarded-For first, need to config physical server)
        String ip = extractClientIp(exchange);

        if (StringUtils.hasText(ip)) {
            return "ip:" + ip;
        }

        // 3. Combine User-Agent + IP to increase uniqueness
        String ua = request.getHeaders().getFirst("User-Agent");

        if (StringUtils.hasText(ua)) {
            return "ua:" + ua.hashCode();
        }

        // 4. Final fallback, aggregates all ambiguous requests to strictly limit
        return "anonymous";
    }

    private String extractClientIp(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");

        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        return Objects.nonNull(request.getRemoteAddress())
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : null;
    }
}
