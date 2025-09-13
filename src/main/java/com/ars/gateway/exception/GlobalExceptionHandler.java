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
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final MessageTranslationUtils messageUtils;

    public GlobalExceptionHandler(MessageTranslationUtils messageUtils) {
        this.messageUtils = messageUtils;
    }

    @Override
    @NonNull
    public Mono<Void> handle(@NonNull ServerWebExchange exchange, @NonNull Throwable e) {
        log.error("[GATEWAY_GLOBAL_EXCEPTION_HANDLER] - Handling error: {}", e.getMessage(), e);
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        BaseResponseDTO.Builder responseBuilder = BaseResponseDTO.builder()
                .code(BaseHttpStatusConstants.INTERNAL_SERVER_ERROR)
                .success(Boolean.FALSE)
                .message(e.getMessage());

        if (BaseException.class.isAssignableFrom(e.getClass())) {
            BaseException exception = (BaseException) e;
            String errorMessage = messageUtils.getMessageI18n(exception.getErrorKey(), exception.getArgs());
            responseBuilder = responseBuilder.message(errorMessage);
        }

        String responseBody = JsonUtils.toJsonString(responseBuilder.build());
        DataBuffer buffer = response.bufferFactory().wrap(responseBody.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
