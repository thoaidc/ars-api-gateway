package com.ars.gateway.config;

import com.ars.gateway.constants.CommonConstants;
import com.dct.model.common.SecurityUtils;
import com.dct.model.config.properties.RateLimiterProps;
import com.dct.model.constants.BaseSecurityConstants;

import org.apache.commons.codec.digest.MessageDigestAlgorithms;
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

        if (StringUtils.hasText(authKey)) {
            log.debug("[RATE_LIMIT_BY_TOKEN] - Using token with key: {}", authKey);
            return authKey;
        }

        // 2. Combine User-Agent + IP
        String uaKey = resolveByUserAgentAndClientIP(exchange);

        if (StringUtils.hasText(uaKey)) {
            log.debug("[RATE_LIMIT_BY_IP] - Using User-Agent + IP with key: {}", uaKey);
            return uaKey;
        }

        // 3. Final fallback, aggregates all ambiguous requests to strictly limit
        log.debug("[RATE_LIMIT_ANONYMOUS_USER] - Resolved rate limit for anonymous user");
        return CommonConstants.ANONYMOUS_USER;
    }

    private String resolveByAuthToken(ServerWebExchange exchange) {
        String token = SecurityUtils.retrieveTokenWebFlux(exchange.getRequest());

        if (StringUtils.hasText(token)) {
            // Hash the token for security (not store full token in Redis)
            return hashString(token);
        }

        return null;
    }

    private String resolveByUserAgentAndClientIP(ServerWebExchange exchange) {
        String userAgent = exchange.getRequest().getHeaders().getFirst(BaseSecurityConstants.HEADER.USER_AGENT);

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
        String xForwardedFor = request.getHeaders().getFirst(BaseSecurityConstants.HEADER.X_FORWARDED_FOR);

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
        String xRealIp = request.getHeaders().getFirst(BaseSecurityConstants.HEADER.X_REAL_IP);

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
            MessageDigest md = MessageDigest.getInstance(MessageDigestAlgorithms.SHA_256);
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
