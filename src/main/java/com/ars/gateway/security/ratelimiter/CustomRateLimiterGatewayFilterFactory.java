package com.ars.gateway.security.ratelimiter;

import com.ars.gateway.constants.ExceptionConstants;
import com.dct.model.common.JsonUtils;
import com.dct.model.common.MessageTranslationUtils;
import com.dct.model.dto.response.BaseResponseDTO;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RequestRateLimiterGatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.HttpStatusHolder;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.setResponseStatus;

/**
 * Custom implementation of {@link AbstractGatewayFilterFactory} that integrates
 * with a {@link CustomRateLimiter} and {@link CustomKeyResolver} to provide
 * request rate limiting for specific routes in Spring Cloud Gateway
 *
 * <p>This factory maps the custom {@link RateLimiterConfig} configuration
 * (defined in application.yml per route) into the standard
 * {@link RequestRateLimiterGatewayFilterFactory.Config} that Spring Cloud Gateway
 * understands, while delegating the rate limiting logic to {@link CustomRateLimiter}
 *
 * <p>Usage example in application.yml:
 * <pre>
 * spring:
 *   cloud:
 *     gateway:
 *       routes:
 *         - id: authenticate-route
 *           uri: service-url
 *           predicates:
 *             - Path=/authenticate
 *           filters:
 *             - name: CustomRateLimiter
 *               args:
 *                 rate-limiter.replenishRate: 30
 *                 rate-limiter.burstCapacity: 50
 *                 rate-limiter.requestedTokens: 1
 * </pre>
 *
 * <p> With this configuration, the factory ensures the route applies custom rate limiting rules
 * using {@link CustomRateLimiter} and identifies clients with {@link CustomKeyResolver}
 * @author thoaidc
 */
@Component
public class CustomRateLimiterGatewayFilterFactory extends AbstractGatewayFilterFactory<RateLimiterConfig> {
    private final CustomRateLimiter customRateLimiter;
    private final CustomKeyResolver customKeyResolver;
    private final MessageTranslationUtils messageTranslationUtils;
    private static final String EMPTY_KEY = "____EMPTY_KEY__";

    /**
     * Creates a new {@code CustomRateLimiterGatewayFilterFactory}
     *
     * @param customRateLimiter the rate limiter implementation that applies
     *                          token-bucket rate limiting logic
     * @param customKeyResolver the resolver used to extract client identifiers
     *                          (e.g., IP, User-Agent, token) for rate limiting
     */
    public CustomRateLimiterGatewayFilterFactory(CustomRateLimiter customRateLimiter,
                                                 CustomKeyResolver customKeyResolver,
                                                 MessageTranslationUtils messageTranslationUtils) {
        super(RateLimiterConfig.class);
        this.customRateLimiter = customRateLimiter;
        this.customKeyResolver = customKeyResolver;
        this.messageTranslationUtils = messageTranslationUtils;
    }

    /**
     * Applies the given {@link RateLimiterConfig} to build a {@link GatewayFilter} instance
     * @param config the custom configuration defined per route in YAML
     * @return a {@link GatewayFilter} that enforces rate limiting rules
     */
    @Override
    public GatewayFilter apply(RateLimiterConfig config) {
        return (exchange, chain) -> this.customKeyResolver.resolve(exchange)
                .defaultIfEmpty(EMPTY_KEY)
                .flatMap(key -> applyResponse(exchange, chain, key));
    }

    private Mono<Void> applyResponse(ServerWebExchange exchange, GatewayFilterChain chain, String key) {
        if (EMPTY_KEY.equals(key)) {
            HttpStatusHolder emptyKeyStatus = HttpStatusHolder.parse(HttpStatus.TOO_MANY_REQUESTS.name());
            setResponseStatus(exchange, emptyKeyStatus);
            return convertResponse(exchange);
        }

        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String routeId = Objects.requireNonNull(route).getId();

        return this.customRateLimiter.isAllowed(routeId, key).flatMap(response -> {
            HttpHeaders headers = exchange.getResponse().getHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            response.getHeaders().forEach(headers::add);

            if (response.isAllowed()) {
                return chain.filter(exchange);
            }

            setResponseStatus(exchange, HttpStatus.TOO_MANY_REQUESTS);
            return convertResponse(exchange);
        });
    }

    private Mono<Void> convertResponse(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        Locale locale = exchange.getLocaleContext().getLocale();
        BaseResponseDTO.Builder responseBuilder = BaseResponseDTO.builder()
                .code(HttpStatus.TOO_MANY_REQUESTS.value())
                .success(Boolean.FALSE)
                .message(messageTranslationUtils.getMessageI18n(locale, ExceptionConstants.TOO_MANY_REQUESTS));
        String responseBody = JsonUtils.toJsonString(responseBuilder.build());
        DataBuffer buffer = response.bufferFactory().wrap(responseBody.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
