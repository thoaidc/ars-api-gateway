package com.ars.gateway.filters;

import com.ars.gateway.common.LocaleUtils;
import com.dct.model.common.MessageTranslationUtils;
import com.dct.model.dto.response.BaseResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.reactivestreams.Publisher;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;

@Component
public class ResponseWrapperFilter implements WebFilter, Ordered {
    private final MessageTranslationUtils messageUtils;
    private final ObjectMapper objectMapper;

    public ResponseWrapperFilter(MessageTranslationUtils messageUtils, ObjectMapper objectMapper) {
        this.messageUtils = messageUtils;
        this.objectMapper = objectMapper;
    }

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpResponse originalResponse = exchange.getResponse();
        LocaleUtils.setLocale(exchange);

        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public @NonNull Mono<Void> writeWith(@NonNull Publisher<? extends DataBuffer> body) {
                if (shouldHandle(originalResponse)) {
                    // Join the entire body into a single buffer
                    Mono<DataBuffer> dataBufferMono = DataBufferUtils.join(Flux.from(body));
                    return dataBufferMono.flatMap(dataBuffer -> handle(originalResponse, dataBuffer));
                }

                return super.writeWith(body);
            }
        };

        ServerWebExchange exchanged = exchange.mutate().response(decoratedResponse).build();
        return chain.filter(exchanged).doFinally(signalType -> LocaleContextHolder.resetLocaleContext());
    }

    private boolean shouldHandle(ServerHttpResponse response) {
        HttpHeaders headers = response.getHeaders();
        MediaType mediaType = headers.getContentType();
        String contentEncoding = headers.getFirst(HttpHeaders.CONTENT_ENCODING);
        Set<MediaType> acceptableMediaTypes = Set.of(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN);

        // If response is compressed/encoded (possible from downstream service) -> do not handle
        if (StringUtils.hasText(contentEncoding)) {
            return false;
        }

        // If Content-Type is not JSON or text/plain -> do not handle
        return Objects.isNull(mediaType) || acceptableMediaTypes.stream().anyMatch(mediaType::isCompatibleWith);
    }

    private Mono<Void> handle(ServerHttpResponse response, DataBuffer dataBuffer) {
        byte[] content = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(content);
        DataBufferUtils.release(dataBuffer);
        String originalBody = new String(content, StandardCharsets.UTF_8);
        String newBody = transformBody(originalBody);
        byte[] newContent = newBody.getBytes(StandardCharsets.UTF_8);
        response.getHeaders().setContentLength(newContent.length);
        DataBuffer buffer = response.bufferFactory().wrap(newContent);
        return response.writeWith(Mono.just(buffer));
    }

    private String transformBody(String originalBody) {
        try {
            BaseResponseDTO responseDTO = objectMapper.readValue(originalBody, BaseResponseDTO.class);
            BaseResponseDTO transformed = messageUtils.setResponseMessageI18n(responseDTO);
            return objectMapper.writeValueAsString(transformed);
        } catch (Exception ignored) {
            return originalBody;
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
