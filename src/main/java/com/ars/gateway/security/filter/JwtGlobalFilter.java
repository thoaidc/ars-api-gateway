package com.ars.gateway.security.filter;

import com.ars.gateway.common.CacheUtils;
import com.ars.gateway.common.Common;
import com.ars.gateway.common.SecurityUtils;
import com.ars.gateway.config.properties.CacheProps;
import com.ars.gateway.config.properties.PublicEndpointProps;

import com.dct.model.common.JsonUtils;
import com.dct.model.constants.BaseHttpStatusConstants;
import com.dct.model.dto.auth.UserDTO;
import com.dct.model.dto.response.BaseResponseDTO;
import com.dct.model.security.BaseJwtProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtGlobalFilter.class);
    private static final String ENTITY_NAME = "JwtGlobalFilter";
    private final BaseJwtProvider jwtProvider;
    private final CacheProps cacheConfig;
    private final CacheUtils cacheUtils;
    private final PublicEndpointProps publicEndpointConfig;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtGlobalFilter(BaseJwtProvider jwtProvider,
                           CacheProps cacheConfig,
                           CacheUtils cacheUtils,
                           PublicEndpointProps publicEndpointConfig) {
        this.jwtProvider = jwtProvider;
        this.cacheConfig = cacheConfig;
        this.cacheUtils = cacheUtils;
        this.publicEndpointConfig = publicEndpointConfig;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = Optional.ofNullable((URI) exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR))
                .map(URI::getPath)
                .orElse(request.getPath().value());

        log.debug("[{}] - Processing request: {} {}", ENTITY_NAME, request.getMethod(), path);

        // Skip authentication cho public endpoints
        if (isPublicEndpoint(path)) {
            log.debug("[{}] - Public endpoint, skipping authentication: {}", ENTITY_NAME, path);
            return chain.filter(exchange);
        }

        // Extract JWT token
        String token = org.apache.commons.lang.StringUtils.trimToNull(SecurityUtils.retrieveTokenFromHeader(request));

        if (Objects.isNull(token)) {
            log.warn("[{}] - Missing token for protected endpoint: {}", ENTITY_NAME, path);
            return handleUnauthorized(exchange, "Authentication token required");
        }

        // Validate token và extract user info
        return validateToken(token).flatMap(authentication -> {
                log.debug("[{}] - Authentication successful for user: {}", ENTITY_NAME, authentication.getName());
                UserDTO userDTO = (UserDTO) authentication.getPrincipal();
                Set<String> userPermissions = userDTO.getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter(StringUtils::hasText)
                        .collect(Collectors.toSet());

                // Add user info vào headers để forward cho downstream services
                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-User-Id", String.valueOf(userDTO.getId()))
                        .header("X-User-Name", userDTO.getUsername())
                        .header("X-User-Permissions", JsonUtils.toJsonString(userPermissions))
                        .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            })
            .onErrorResume(error -> handleUnauthorized(exchange, error));
    }

    private boolean isPublicEndpoint(String fullPath) {
        String normalizedPath = Common.normalizePath(fullPath);

        if (Objects.isNull(normalizedPath))
            return false;

        return publicEndpointConfig.getPublicPatterns()
                .stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, normalizedPath));
    }

    private Mono<Authentication> validateToken(String token) {
        if (cacheConfig.isEnabled()) {
            return Mono.fromCallable(() -> cacheUtils.getCache(token, Authentication.class))
                    .subscribeOn(Schedulers.boundedElastic())
                    .switchIfEmpty(
                        // If not in cache, validate token and cache result
                        Mono.fromCallable(() -> jwtProvider.validateToken(token))
                            .subscribeOn(Schedulers.boundedElastic())
                            .doOnNext(authentication -> cacheUtils.cache(token, authentication))
                    );
        }

        return Mono.fromCallable(() -> jwtProvider.validateToken(token)).subscribeOn(Schedulers.boundedElastic());
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

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, Throwable exception) {
        return handleUnauthorized(exchange, exception.getMessage());
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
