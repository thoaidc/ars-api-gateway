package com.ars.gateway.security.filter;

import com.ars.gateway.constants.CommonConstants;
import com.dct.model.common.JsonUtils;
import com.dct.model.common.SecurityUtils;
import com.dct.model.dto.auth.BaseUserDTO;
import com.dct.model.dto.response.BaseResponseDTO;
import com.dct.model.exception.BaseException;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class PreAuthorizeFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(PreAuthorizeFilter.class);
    private final JwtProvider jwtProvider;

    public PreAuthorizeFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = SecurityUtils.retrieveTokenWebFlux(exchange.getRequest());

        if (StringUtils.hasText(token)) {
            return jwtProvider.validateToken(token)
                    .flatMap(userDTO -> setAuthentication(exchange, chain, (BaseUserDTO) userDTO))
                    .onErrorResume(error -> handleUnauthorized(exchange, error));
        }

        return chain.filter(exchange);
    }

    private Mono<Void> setAuthentication(ServerWebExchange exchange, GatewayFilterChain chain, BaseUserDTO userDTO) {
        exchange.getAttributes().put(CommonConstants.AUTHENTICATION_EXCHANGE_ATTRIBUTE, userDTO);
        return chain.filter(exchange);
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, Throwable e) {
        // Handle custom authenticate exception
        if (e instanceof JwtException || e instanceof BaseException) {
            log.error("[GATEWAY_JWT_FILTER_ERROR] - Token validation failed: {}", e.getMessage());
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            BaseResponseDTO responseDTO = BaseResponseDTO.builder()
                    .code(HttpStatus.UNAUTHORIZED.value())
                    .success(Boolean.FALSE)
                    .message("Unauthorized request! Your token was invalid or expired")
                    .build();
            String responseBody = JsonUtils.toJsonString(responseDTO);
            DataBuffer buffer = response.bufferFactory().wrap(responseBody.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        }

        return Mono.error(e);
    }

    /**
     * Set order < RequestRateLimiterGatewayFilterFactory (usually HIGHEST_PRECEDENCE + 1)
     * so it runs BEFORE the rate limiter filter (-2)
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}
