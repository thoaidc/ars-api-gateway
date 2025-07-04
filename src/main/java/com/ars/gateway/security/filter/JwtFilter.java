package com.ars.gateway.security.filter;

import com.ars.gateway.common.SecurityUtils;
import com.ars.gateway.config.properties.PublicEndpointProps;
import com.ars.gateway.constants.CommonConstants;
import com.dct.model.common.JsonUtils;
import com.dct.model.constants.BaseHttpStatusConstants;
import com.dct.model.dto.response.BaseResponseDTO;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Component
public class JwtFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);
    private static final String ENTITY_NAME = "JwtFilter";
    private final JwtProvider jwtProvider;
    private final List<PathPattern> publicPatterns;

    public JwtFilter(JwtProvider jwtProvider, PublicEndpointProps publicEndpointProps) {
        this.jwtProvider = jwtProvider;
        PathPatternParser parser = new PathPatternParser();
        this.publicPatterns = Arrays.stream(publicEndpointProps.getPublicPatterns()).map(parser::parse).toList();
    }

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        log.debug("[{}] - JWT filtering request: {}", ENTITY_NAME, path);

        if (ifAuthenticationNotRequired(path)) {
            return chain.filter(exchange);
        }

        String token = StringUtils.trimToNull(SecurityUtils.retrieveTokenFromHeader(exchange.getRequest()));

        return jwtProvider.validateToken(token)
                .flatMap(authentication -> setAuthentication(exchange, chain, authentication))
                .onErrorResume(error -> handleUnauthorized(exchange, error));
    }

    private boolean ifAuthenticationNotRequired(String path) {
        return publicPatterns.stream().anyMatch(p -> p.matches(PathContainer.parsePath(path)));
    }

    private Mono<Void> setAuthentication(ServerWebExchange exchange, WebFilterChain chain, Authentication auth) {
        exchange.getAttributes().put(CommonConstants.AUTHENTICATION_EXCHANGE_ATTRIBUTE, auth);
        return chain.filter(exchange).contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, Throwable e) {
        log.error("[{}] - Token validation failed: {}", ENTITY_NAME, e.getMessage());
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        BaseResponseDTO responseDTO = BaseResponseDTO.builder()
                .code(BaseHttpStatusConstants.UNAUTHORIZED)
                .success(BaseHttpStatusConstants.STATUS.FAILED)
                .message("Unauthorized request! Your token was invalid or expired.")
                .build();

        String responseBody = JsonUtils.toJsonString(responseDTO);
        DataBuffer buffer = response.bufferFactory().wrap(responseBody.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
