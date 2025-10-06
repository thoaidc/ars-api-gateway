package com.ars.gateway.filters;

import com.ars.gateway.common.LocaleUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LocaleContextHolderWebFilter implements WebFilter {
    /**
     * Set locale by 'Accept-Language' header from request to use i18n translation
     * @param exchange the current server exchange
     * @param chain provides a way to delegate to the next filter
     * @author thoaidc
     */
    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, WebFilterChain chain) {
        LocaleUtils.setLocale(exchange);
        return chain.filter(exchange).doFinally(signalType -> LocaleContextHolder.resetLocaleContext());
    }
}
