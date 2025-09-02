package com.ars.gateway.security.filter;

import com.ars.gateway.constants.CommonConstants;
import com.dct.model.common.JsonUtils;
import com.dct.model.common.SecurityUtils;
import com.dct.model.config.properties.SecurityProps;
import com.dct.model.constants.BaseHttpStatusConstants;
import com.dct.model.dto.response.BaseResponseDTO;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
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
import java.nio.charset.StandardCharsets;

@Component
public class JwtFilter implements WebFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);
    private final JwtProvider jwtProvider;
    private final String[] publicPatterns;

    public JwtFilter(JwtProvider jwtProvider, SecurityProps securityProps) {
        this.jwtProvider = jwtProvider;
        this.publicPatterns = securityProps.getPublicRequestPatterns();
    }

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        log.debug("[GATEWAY_JWT_FILTER] - Filtering request: {}", path);

        if (SecurityUtils.checkIfAuthenticationNotRequired(path, publicPatterns)) {
            return chain.filter(exchange);
        }

        String token = StringUtils.trimToNull(SecurityUtils.retrieveTokenWebFlux(exchange.getRequest()));

        return jwtProvider.validateToken(token)
                .flatMap(authentication -> setAuthentication(exchange, chain, authentication))
                .onErrorResume(error -> handleUnauthorized(exchange, error));
    }

    private Mono<Void> setAuthentication(ServerWebExchange exchange, WebFilterChain chain, Authentication auth) {
        exchange.getAttributes().put(CommonConstants.AUTHENTICATION_EXCHANGE_ATTRIBUTE, auth);
        return chain.filter(exchange).contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, Throwable e) {
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
}
