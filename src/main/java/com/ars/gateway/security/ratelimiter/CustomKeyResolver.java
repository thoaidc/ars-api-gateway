package com.ars.gateway.security.ratelimiter;

import com.ars.gateway.common.EncryptionUtils;
import com.ars.gateway.constants.CommonConstants;
import com.ars.gateway.constants.RateLimitConstants;
import com.ars.gateway.dto.CheckValidDeviceIdResponseDTO;
import com.dct.model.constants.BaseSecurityConstants;
import com.dct.model.dto.auth.BaseUserDTO;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Optional;

@Component("customKeyResolver")
public class CustomKeyResolver implements KeyResolver {
    private final EncryptionUtils encryptionUtils;

    public CustomKeyResolver(EncryptionUtils encryptionUtils) {
        this.encryptionUtils = encryptionUtils;
    }

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        BaseUserDTO userDTO = exchange.getAttribute(CommonConstants.AUTHENTICATION_EXCHANGE_ATTRIBUTE);
        Integer userId = Optional.ofNullable(userDTO).orElseGet(BaseUserDTO::new).getId();
        String deviceId = exchange.getRequest().getHeaders().getFirst(BaseSecurityConstants.HEADER.X_DEVICE_ID);
        CheckValidDeviceIdResponseDTO result = encryptionUtils.checkValidDeviceId(deviceId);
        String rateLimitKey = "";

        if (result.isValid() && StringUtils.hasText(result.getDeviceId())) {
            rateLimitKey = rateLimitKey.concat(RateLimitConstants.DEVICE_BANNED_KEY).concat(result.getDeviceId());
        }

        if (Objects.nonNull(userId)) {
            rateLimitKey = rateLimitKey.concat(RateLimitConstants.USER_ID_BANNED_KEY).concat(String.valueOf(userId));
        }

        if (StringUtils.hasText(rateLimitKey)) {
            return Mono.just(rateLimitKey);
        }

        String userIP = Optional.ofNullable(extractClientIp(exchange)).orElse(CommonConstants.ANONYMOUS_USER);
        rateLimitKey += RateLimitConstants.IP_BANNED_KEY + userIP;
        return Mono.just(rateLimitKey);
    }

    private String extractClientIp(ServerWebExchange exchange) {
        try {
            ServerHttpRequest request = exchange.getRequest();
            return Objects.requireNonNull(request.getRemoteAddress()).getAddress().getHostAddress();
        } catch (Exception ignored) {}

        return null;
    }
}
