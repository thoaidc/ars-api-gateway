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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class AccessDeniedHandler implements ServerAccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(AccessDeniedHandler.class);
    private static final String ENTITY_NAME = "AccessDeniedHandler";

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
        log.error("[{}] - Access denied handler is active: {}", ENTITY_NAME, denied.getMessage());
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        BaseResponseDTO responseDTO = BaseResponseDTO.builder()
                .code(BaseHttpStatusConstants.FORBIDDEN)
                .success(BaseHttpStatusConstants.STATUS.FAILED)
                .message(denied.getMessage())
                .build();

        String responseBody = JsonUtils.toJsonString(responseDTO);
        DataBuffer buffer = response.bufferFactory().wrap(responseBody.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
