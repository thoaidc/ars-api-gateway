package com.ars.gateway.filters;

import com.ars.gateway.common.LocaleUtils;
import com.ars.gateway.constants.CommonConstants;
import com.dct.model.common.MessageTranslationUtils;
import com.dct.model.dto.response.BaseResponseDTO;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
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
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A global {@link WebFilter} that intercepts HTTP requests and responses in a Spring WebFlux application <p>
 *
 * <b>Flow and Capabilities:</b>
 * <ul>
 *     <li>
 *         This filter runs <b>before the request is routed to any downstream service</b>,
 *         meaning it can inspect and modify request headers and query parameters before the downstream routing filter
 *         (such as Gateway routing filters) is executed, but <b>cannot modify the body of requests going downstream</b>
 *     </li>
 *     <li>
 *         It also wraps the {@link ServerHttpResponse}, allowing it to intercept and modify the response
 *         <b>after it returns from downstream services or internal controllers</b>, but <b>before it is sent to the client</b>
 *     </li>
 *     <li>
 *         Effectively, it can only read the request headers/body prior to downstream routing, but can fully inspect
 *         and transform the response after controller or once it comes back from downstream or controller processing
 *     </li>
 * </ul>
 *
 * <b>In this case:</b>
 * <ul>
 *     <li>Only handle responses from internal gateway controllers</li>
 *     <li>Localize message for response according to i18n standard</li>
 *     <li>
 *         Ignore response transformations if they are from downstream services
 *         or have content that is not in the JSON raw or {@link BaseResponseDTO} format
 *     </li>
 * </ul>
 *
 * <p>
 * <b>Integration details:</b>
 * <ul>
 *     <li>Registered as a Spring Bean via {@link Component}, applied globally</li>
 *     <li>Implements {@link Ordered} with level = LOWEST_PRECEDENCE, so other filters (e.g., security) run first</li>
 *     <li>Locale is set per request via {@link LocaleUtils#setLocale} and reset after the response completes</li>
 * </ul>
 *
 * <p><b>Example usage:</b>
 * <pre>
 * {@code
 *      @RestController
 *      public class MyController {
 *          @GetMapping("/api/data")
 *          public Mono<BaseResponseDTO> getData() {
 *              return Mono.just(new BaseResponseDTO(...));
 *          }
 *      }
 * }
 * </pre>
 *
 * The response body from this controller will automatically be processed by
 * ResponseWrapperFilter to include translated messages if available
 * </p>
 *
 * @author thoaidc
 */
@Component
public class ResponseWrapperFilter implements WebFilter, Ordered {
    private final Logger log = LoggerFactory.getLogger(ResponseWrapperFilter.class);
    private final Set<MediaType> acceptableMediaTypes = Set.of(MediaType.APPLICATION_JSON);
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
        String isDownstreamServiceHeader = headers.getFirst(CommonConstants.DOWNSTREAM_SERVICE_HEADER);

        // Check if response header is attached from downstream gateway routing filter -> do not handle
        if (StringUtils.hasText(isDownstreamServiceHeader)) {
            return false;
        }

        List<String> contentEncodings = headers.get(HttpHeaders.CONTENT_ENCODING);
        List<String> mediaTypes = headers.get(HttpHeaders.CONTENT_TYPE);

        // If response is compressed/encoded (possible from downstream service) -> do not handle
        if (Objects.nonNull(contentEncodings) && !contentEncodings.isEmpty()) {
            return false;
        }

        // If the data type cannot be determined (due to media type null) - > do not handle
        if (Objects.isNull(mediaTypes) || mediaTypes.isEmpty()) {
            return false;
        }

        // If it contains any type other than JSON -> do not handle
        return mediaTypes.stream().allMatch(this::isAcceptableMediaType);
    }

    private boolean isAcceptableMediaType(String mediaTypeStr) {
        try {
            MediaType mediaType = MediaType.parseMediaType(mediaTypeStr.trim());
            return acceptableMediaTypes.contains(mediaType);
        } catch (InvalidMediaTypeException e) {
            return false;
        }
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
            // Create a separate ObjectMapper to fail if a field does not exist in the BaseResponseDTO
            ObjectMapper strictMapper = objectMapper.copy()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

            BaseResponseDTO responseDTO = strictMapper.readValue(originalBody, BaseResponseDTO.class);
            BaseResponseDTO transformed = messageUtils.setResponseMessageI18n(responseDTO);
            return objectMapper.writeValueAsString(transformed);
        } catch (UnrecognizedPropertyException e) {
            // There is a field not in BaseResponseDTO -> ignore, return raw
            log.debug("Response JSON contains unknown fields -> returning raw response");
        } catch (Exception ignored) {
            log.debug("Response JSON not valid BaseResponseDTO -> returning raw response");
        }

        return originalBody;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
