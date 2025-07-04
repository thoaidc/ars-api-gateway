package com.ars.gateway.exception;

import com.dct.model.common.JsonUtils;
import com.dct.model.common.MessageTranslationUtils;
import com.dct.model.constants.BaseHttpStatusConstants;
import com.dct.model.dto.response.BaseResponseDTO;
import com.dct.model.exception.BaseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@Order(-2)
public class GlobalErrorHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorHandler.class);
    private static final String ENTITY_NAME = "GlobalErrorHandler";
    private final MessageTranslationUtils messageUtils;

    public GlobalErrorHandler(MessageTranslationUtils messageUtils) {
        this.messageUtils = messageUtils;
    }

    @Override
    @NonNull
    public Mono<Void> handle(@NonNull ServerWebExchange exchange, @NonNull Throwable e) {
        log.error("[{}] - Handling error: {}", ENTITY_NAME, e.getMessage());
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        BaseResponseDTO.Builder builder = BaseResponseDTO.builder()
                .code(BaseHttpStatusConstants.INTERNAL_SERVER_ERROR)
                .success(BaseHttpStatusConstants.STATUS.FAILED);
        BaseResponseDTO responseDTO = builder.message(e.getMessage()).build();

        if (BaseException.class.isAssignableFrom(e.getClass())) {
            BaseException exception = (BaseException) e;
            responseDTO = builder.code(exception.getCode())
                    .message(messageUtils.getMessageI18n(exception.getErrorKey(), exception.getArgs()))
                    .build();
        }

        String responseBody = JsonUtils.toJsonString(responseDTO);
        DataBuffer buffer = response.bufferFactory().wrap(responseBody.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
