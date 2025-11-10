package com.ars.gateway.security.filter;

import com.ars.gateway.constants.CommonConstants;
import com.ars.gateway.security.config.DynamicPublicRequestContext;
import com.dct.model.common.JsonUtils;
import com.dct.model.common.SecurityUtils;
import com.dct.model.constants.BaseHttpStatusConstants;
import com.dct.model.dto.auth.BaseUserDTO;
import com.dct.model.dto.response.BaseResponseDTO;
import com.dct.model.exception.BaseException;

import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;
import java.nio.charset.StandardCharsets;

@Component
public class JwtFilter implements WebFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);
    private final DynamicPublicRequestContext dynamicPublicRequestContext;
    private final JwtProvider jwtProvider;

    public JwtFilter(DynamicPublicRequestContext dynamicPublicRequestContext, JwtProvider jwtProvider) {
        this.dynamicPublicRequestContext = dynamicPublicRequestContext;
        this.jwtProvider = jwtProvider;
    }

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        log.debug("[GATEWAY_JWT_FILTER] - Filtering request: {} {}", exchange.getRequest().getMethod(), path);

        if (dynamicPublicRequestContext.isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String token = SecurityUtils.retrieveTokenWebFlux(exchange.getRequest());
        exchange.getAttributes().put(CommonConstants.TOKEN_EXCHANGE_ATTRIBUTE, token);

        return jwtProvider.validateToken(token)
                .flatMap(userDTO -> setAuthentication(exchange, chain, userDTO))
                .onErrorResume(error -> handleUnauthorized(exchange, error));
    }

    private Mono<Void> setAuthentication(ServerWebExchange exchange, WebFilterChain chain, BaseUserDTO userDTO) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            userDTO,
            userDTO.getUsername(),
            userDTO.getAuthorities()
        );
        System.out.println(userDTO.getAuthorities());
        exchange.getAttributes().put(CommonConstants.AUTHENTICATION_EXCHANGE_ATTRIBUTE, userDTO);
        return chain.filter(exchange).contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, Throwable e) {
        // Handle custom authenticate exception
        if (e instanceof JwtException || e instanceof BaseException) {
            log.error("[GATEWAY_JWT_FILTER_ERROR] - Token validation failed: {}", e.getMessage());
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            BaseResponseDTO responseDTO = BaseResponseDTO.builder()
                    .code(BaseHttpStatusConstants.UNAUTHORIZED)
                    .success(Boolean.FALSE)
                    .message("Unauthorized request! Your token was invalid or expired.")
                    .build();
            String responseBody = JsonUtils.toJsonString(responseDTO);
            DataBuffer buffer = response.bufferFactory().wrap(responseBody.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        }

        return Mono.error(e);
    }
}
