package com.ars.gateway.security.filter;

import com.ars.gateway.common.CacheUtils;
import com.dct.model.config.properties.RedisProps;
import com.dct.model.config.properties.SecurityProps;
import com.dct.model.constants.ActivateStatus;
import com.dct.model.constants.BaseExceptionConstants;
import com.dct.model.constants.BaseSecurityConstants;
import com.dct.model.dto.auth.BaseUserDTO;
import com.dct.model.exception.BaseBadRequestException;
import com.dct.model.security.AbstractJwtProvider;

import io.jsonwebtoken.Claims;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtProvider extends AbstractJwtProvider {
    private static final String ENTITY_NAME = "com.ars.gateway.security.filter.JwtProvider";
    private final CacheUtils cacheUtils;
    private final RedisProps redisProps;

    public JwtProvider(SecurityProps securityProps, CacheUtils cacheUtils, RedisProps redisProps) {
        super(securityProps);
        this.cacheUtils = cacheUtils;
        this.redisProps = redisProps;
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
        boolean isCacheEnabled = ActivateStatus.ENABLED.equals(redisProps.getActivate());
        return Mono.fromCallable(() -> super.parseToken(token))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(authentication -> {
                    if (isCacheEnabled) {
                        cacheUtils.cache(token, authentication);
                    }
                });
    }

    @Override
    protected Authentication getAuthentication(Claims claims) {
        Integer userId = (Integer) claims.get(BaseSecurityConstants.TOKEN_PAYLOAD.USER_ID);
        String username = (String) claims.get(BaseSecurityConstants.TOKEN_PAYLOAD.USERNAME);
        String authorities = (String) claims.get(BaseSecurityConstants.TOKEN_PAYLOAD.AUTHORITIES);

        Set<SimpleGrantedAuthority> userAuthorities = Arrays.stream(authorities.split(","))
                .filter(StringUtils::hasText)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());

        BaseUserDTO principal = BaseUserDTO.userBuilder()
                .withId(userId)
                .withUsername(username)
                .withAuthorities(userAuthorities)
                .build();

        return new UsernamePasswordAuthenticationToken(principal, username, userAuthorities);
    }
}
