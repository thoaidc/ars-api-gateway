package com.ars.gateway.security.filter;

import com.ars.gateway.common.CacheUtils;
import com.ars.gateway.common.SecurityUtils;
import com.ars.gateway.config.properties.CacheProps;
import com.dct.model.common.JsonUtils;
import com.dct.model.constants.BaseHttpStatusConstants;
import com.dct.model.dto.response.BaseResponseDTO;
import com.dct.model.security.BaseJwtProvider;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Component
public class JwtFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);
    private static final String ENTITY_NAME = "JwtFilter";
    private final BaseJwtProvider jwtProvider;
    private final CacheProps cacheConfig;
    private final CacheUtils cacheUtils;

    public JwtFilter(BaseJwtProvider jwtProvider, CacheProps cacheConfig, CacheUtils cacheUtils) {
        this.jwtProvider = jwtProvider;
        this.cacheConfig = cacheConfig;
        this.cacheUtils = cacheUtils;
    }

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        // Extract JWT token
        String token = StringUtils.trimToNull(SecurityUtils.retrieveTokenFromHeader(exchange.getRequest()));

        if (Objects.isNull(token)) {
            log.warn("[{}] - Missing token for protected endpoint", ENTITY_NAME);
            return handleUnauthorized(exchange, "Authentication token required");
        }

        return validateToken(token)
                .flatMap(authentication ->
                    chain.filter(exchange).contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
                )
                .onErrorResume(e -> {
                    log.error("[{}] - Token validation failed: {}", ENTITY_NAME, e.getMessage());
                    return handleUnauthorized(exchange, "Invalid or expired token");
                });
    }

    private Mono<Authentication> validateToken(String token) {
        if (cacheConfig.isEnabled()) {
            return Mono.fromCallable(() -> cacheUtils.getCache(token, Authentication.class))
                    .subscribeOn(Schedulers.boundedElastic())
                    .switchIfEmpty(validateAndCacheToken(token));
        }

        return Mono.fromCallable(() -> jwtProvider.validateToken(token)).subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<Authentication> validateAndCacheToken(String token) {
        return Mono.fromCallable(() -> jwtProvider.validateToken(token))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(authentication -> cacheUtils.cache(token, authentication));
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        log.error("[{}] - Authentication failed: {}", ENTITY_NAME, message);
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        BaseResponseDTO responseDTO = BaseResponseDTO.builder()
                .code(BaseHttpStatusConstants.UNAUTHORIZED)
                .success(BaseHttpStatusConstants.STATUS.FAILED)
                .message(message)
                .build();

        String responseBody = JsonUtils.toJsonString(responseDTO);
        DataBuffer buffer = response.bufferFactory().wrap(responseBody.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
