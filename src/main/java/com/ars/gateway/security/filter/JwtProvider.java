package com.ars.gateway.security.filter;

import com.ars.gateway.common.CacheUtils;
import com.dct.model.config.properties.SecurityProps;
import com.dct.model.constants.BaseExceptionConstants;
import com.dct.model.exception.BaseBadRequestException;
import com.dct.model.security.AbstractJwtProvider;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class JwtProvider extends AbstractJwtProvider {
    private static final String ENTITY_NAME = "com.ars.gateway.security.filter.JwtProvider";
    private final CacheUtils cacheUtils;

    public JwtProvider(SecurityProps securityProps, CacheUtils cacheUtils) {
        super(securityProps);
        this.cacheUtils = cacheUtils;
    }

    public Mono<Authentication> validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            return Mono.error(new BaseBadRequestException(ENTITY_NAME, BaseExceptionConstants.BAD_CREDENTIALS));
        }

        return getAuthenticationFromCacheOrParse(token);
    }

    private Mono<Authentication> getAuthenticationFromCacheOrParse(String token) {
        return Mono.fromCallable(() -> (Authentication) cacheUtils.get(token, UsernamePasswordAuthenticationToken.class))
                .subscribeOn(Schedulers.boundedElastic())
                .switchIfEmpty(parseAndCache(token));
    }

    private Mono<Authentication> parseAndCache(String token) {
        return Mono.fromCallable(() -> super.validateAccessToken(token))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(authentication -> cacheUtils.cache(token, authentication));
    }
}
