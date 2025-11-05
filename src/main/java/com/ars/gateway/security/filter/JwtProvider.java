package com.ars.gateway.security.filter;

import com.dct.model.config.properties.SecurityProps;
import com.dct.model.constants.BaseExceptionConstants;
import com.dct.model.constants.BaseSecurityConstants;
import com.dct.model.dto.auth.BaseUserDTO;
import com.dct.model.exception.BaseBadRequestException;
import com.dct.model.exception.BaseIllegalArgumentException;
import com.dct.model.security.AbstractJwtProvider;

import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(JwtProvider.class);

    public JwtProvider(SecurityProps securityProps) {
        super(securityProps);
    }

    public Mono<BaseUserDTO> validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            return Mono.error(new BaseBadRequestException(ENTITY_NAME, BaseExceptionConstants.BAD_CREDENTIALS));
        }

        return Mono.fromCallable(() -> getAuthentication(token)).subscribeOn(Schedulers.boundedElastic());
    }

    private BaseUserDTO getAuthentication(String token) {
        try {
            Claims claims = parseToken(super.accessTokenParser, token);
            Integer userId = (Integer) claims.get(BaseSecurityConstants.TOKEN_PAYLOAD.USER_ID);
            Integer shopId = (Integer) claims.get(BaseSecurityConstants.TOKEN_PAYLOAD.SHOP_ID);
            String username = (String) claims.get(BaseSecurityConstants.TOKEN_PAYLOAD.USERNAME);
            String authoritiesStr = (String) claims.get(BaseSecurityConstants.TOKEN_PAYLOAD.AUTHORITIES);
            Set<String> authorities = Arrays.stream(authoritiesStr.split(","))
                    .map(String::trim).
                    collect(Collectors.toSet());

            return BaseUserDTO.userBuilder()
                    .withId(userId)
                    .withShopId(shopId)
                    .withUsername(username)
                    .withAuthorities(authorities)
                    .build();
        } catch (Exception e) {
            log.error("[JWT_PROVIDER_GET_AUTHENTICATION_ERROR] - error: {}", e.getMessage());
            throw new BaseIllegalArgumentException(ENTITY_NAME, "Could not get authentication from token");
        }
    }
}
