package com.ars.gateway.security.handler;

import com.dct.model.common.JsonUtils;
import com.dct.model.constants.BaseHttpStatusConstants;
import com.dct.model.dto.response.BaseResponseDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class AuthenticationEntryPoint implements ServerAuthenticationEntryPoint {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationEntryPoint.class);

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        log.error("[AUTHENTICATION_ENTRYPOINT] - error: {}", ex.getMessage());
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        BaseResponseDTO responseDTO = BaseResponseDTO.builder()
                .code(BaseHttpStatusConstants.UNAUTHORIZED)
                .success(Boolean.FALSE)
                .message(ex.getMessage())
                .build();

        String responseBody = JsonUtils.toJsonString(responseDTO);
        DataBuffer buffer = response.bufferFactory().wrap(responseBody.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
