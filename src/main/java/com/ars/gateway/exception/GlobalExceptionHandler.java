package com.ars.gateway.exception;

import com.ars.gateway.constants.FilterChainConstants;
import com.dct.model.common.JsonUtils;
import com.dct.model.common.MessageTranslationUtils;
import com.dct.model.constants.BaseExceptionConstants;
import com.dct.model.dto.response.BaseResponseDTO;
import com.dct.model.exception.BaseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.boot.autoconfigure.web.reactive.error.DefaultErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Global exception handler for Spring WebFlux / Spring Cloud Gateway
 *
 * <p>Purpose:
 * <ul>
 *     <li>Override Spring WebFlux’s default error handler.</li>
 *     <li>Normalize all error responses into the {@link BaseResponseDTO} format.</li>
 *     <li>Catch any unhandled exception thrown anywhere in the Gateway request pipeline.</li>
 * </ul>
 *
 * <p>Why @{@link Order}({@link FilterChainConstants.Order#BEFORE_SPRING_DEFAULT_GLOBAL_ERROR_HANDLER})?
 * <ul>
 *     <li>Spring WebFlux registers {@link DefaultErrorWebExceptionHandler} with @{@link Order}(-1).</li>
 *     <li>To ensure this handler executes first, its order must be smaller (e.g., -2).</li>
 *     <li>If the order is higher (-1, 0, 1...), the default handler will consume the exception
 *         and this handler will never run.</li>
 * </ul>
 *
 * <p>When this handler is triggered:
 * <ul>
 *     <li>When any exception escapes the {@link WebFilterChain} or Gateway filter chain</li>
 *     <li>When Spring Cloud Gateway fails during routing:
 *         <ul>
 *             <li>No available downstream service instance.</li>
 *             <li>Connection errors when calling downstream ({@link java.net.ConnectException}).</li>
 *             <li>Routing or connection timeouts.</li>
 *             <li>Invalid route definitions.</li>
 *         </ul>
 *     </li>
 *     <li>When any WebFlux handler, filter, or internal component throws an exception that is not handled.</li>
 * </ul>
 *
 * <p>When this handler is NOT triggered:
 * <ul>
 *     <li>When the response has already been committed before the exception occurs.</li>
 *     <li>When a custom GlobalFilter handles the exception using onErrorResume.</li>
 *     <li>When another {@link ErrorWebExceptionHandler} with a lower @Order handles the exception first.</li>
 * </ul>
 *
 * <p>How it works:
 * <ol>
 *     <li>Any unhandled exception in WebFlux triggers the {@link ErrorWebExceptionHandler} chain.</li>
 *     <li>Handlers are executed in ascending order (lowest value first).</li>
 *     <li>This handler writes a JSON error response directly to the {@link ServerHttpResponse} and terminates the pipeline.</li>
 * </ol>
 *
 * <p>Result: all unhandled Gateway errors are returned in a consistent JSON structure
 *
 * @author thoaidc
 */
@Component
@Order(FilterChainConstants.Order.BEFORE_SPRING_DEFAULT_GLOBAL_ERROR_HANDLER)
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final MessageTranslationUtils messageUtils;

    public GlobalExceptionHandler(MessageTranslationUtils messageUtils) {
        this.messageUtils = messageUtils;
    }

    @Override
    @NonNull
    public Mono<Void> handle(@NonNull ServerWebExchange exchange, @NonNull Throwable e) {
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethod().name();
        String fullUrl = request.getURI().toString();
        log.error("[GATEWAY_GLOBAL_EXCEPTION_HANDLER_INFO] - Request: {} {}", method, fullUrl);
        log.error("[GATEWAY_GLOBAL_EXCEPTION_HANDLER] - Handling error: {}", e.getMessage(), e);
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        BaseResponseDTO.Builder responseBuilder = BaseResponseDTO.builder()
                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .success(Boolean.FALSE)
                .message(messageUtils.getMessageI18n(BaseExceptionConstants.UNCERTAIN_ERROR));

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
