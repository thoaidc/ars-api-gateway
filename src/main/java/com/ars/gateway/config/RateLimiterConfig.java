package com.ars.gateway.config;

import com.ars.gateway.config.properties.RateLimiterProps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;

@Configuration
@EnableConfigurationProperties(RateLimiterProps.class)
public class RateLimiterConfig {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterConfig.class);
    private static final String ENTITY_NAME = "RateLimiterConfig";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String X_REAL_IP = "X-Real-IP";
    private static final String USER_AGENT = "User-Agent";
    private final RateLimiterProps rateLimiterConfigs;

    public RateLimiterConfig(RateLimiterProps rateLimiterConfigs) {
        this.rateLimiterConfigs = rateLimiterConfigs;
    }

    @Bean("compositeKeyResolver")
    public KeyResolver compositeKeyResolver() {
        return exchange -> Mono.just(resolveKey(exchange));
    }

    private String resolveKey(ServerWebExchange exchange) {
        // 1. Prioritize processing Authorization tokens first
        String authKey = resolveByAuthToken(exchange);

        if (authKey != null) {
            log.debug("[{}] - Resolved rate limit using token with key: {}", ENTITY_NAME, authKey);
            return authKey;
        }

        // 2. Combine User-Agent + IP
        String uaKey = resolveByUserAgentAndClientIP(exchange);

        if (uaKey != null) {
            log.debug("[{}] - Resolved rate limit using User-Agent + IP with key: {}", ENTITY_NAME, uaKey);
            return uaKey;
        }

        // 3. Final fallback, aggregates all ambiguous requests to strictly limit
        log.debug("[{}] - Resolved rate limit for anonymous user", ENTITY_NAME);
        return "anonymous";
    }

    private String resolveByAuthToken(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length()).trim();

            if (StringUtils.hasText(token)) {
                // Hash the token for security (not store full token in Redis)
                String hashedToken = hashString(token);
                return "token:" + hashedToken;
            }
        }

        return null;
    }

    private String resolveByUserAgentAndClientIP(ServerWebExchange exchange) {
        String userAgent = exchange.getRequest().getHeaders().getFirst(USER_AGENT);

        if (StringUtils.hasText(userAgent)) {
            // Get IP as additional context for User-Agent based limiting
            String ip = extractClientIp(exchange);
            String combined = userAgent + (ip != null ? ":" + ip : "");
            return "ua:" + hashString(combined);
        } else {
            String ip = extractClientIp(exchange);

            if (StringUtils.hasText(ip) && isValidIp(ip)) {
                return "ip:" + ip;
            }
        }

        return null;
    }

    private String extractClientIp(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        // Check X-Forwarded-For first (most common proxy header)
        String xForwardedFor = request.getHeaders().getFirst(X_FORWARDED_FOR);

        if (StringUtils.hasText(xForwardedFor)) {
            // Get the first IP in the chain (original client IP)
            String[] ips = xForwardedFor.split(",");
            for (String ip : ips) {
                String cleanIp = ip.trim();
                if (isValidIp(cleanIp)) {
                    return cleanIp;
                }
            }
        }

        // Check X-Real-IP header (nginx proxy)
        String xRealIp = request.getHeaders().getFirst(X_REAL_IP);

        if (StringUtils.hasText(xRealIp) && isValidIp(xRealIp.trim())) {
            return xRealIp.trim();
        }

        // Fallback to remote address
        if (Objects.nonNull(request.getRemoteAddress())) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }

        return null;
    }

    private boolean isValidIp(String ip) {
        if (!StringUtils.hasText(ip)) {
            return false;
        }

        // Check if IP validation is disabled
        if (!rateLimiterConfigs.isIpValidationEnabled()) {
            return true;
        }

        // Check against excluded IPs list
        List<String> excludedIps = rateLimiterConfigs.getExcludedIps();

        if (excludedIps != null) {
            for (String excludedIp : excludedIps) {
                if (ip.equals(excludedIp)) {
                    return false;
                }
            }
        }

        // Check against excluded IP prefixes list
        List<String> excludedPrefixes = rateLimiterConfigs.getExcludedIpPrefixes();

        if (excludedPrefixes != null) {
            for (String prefix : excludedPrefixes) {
                if (ip.startsWith(prefix)) {
                    return false;
                }
            }
        }

        return true;
    }

    private String hashString(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            StringBuilder sb = new StringBuilder();
            byte[] hash = md.digest(input.getBytes());

            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }

            return sb.substring(0, 32); // Use first 32 chars for shorter key
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple hashCode if SHA-256 is not available
            return String.valueOf(Math.abs(input.hashCode()));
        }
    }
}
