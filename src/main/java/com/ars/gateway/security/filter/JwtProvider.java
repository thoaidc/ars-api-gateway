package com.ars.gateway.security.filter;

import com.dct.model.config.properties.JwtProps;
import com.dct.model.constants.BaseExceptionConstants;
import com.dct.model.constants.BaseSecurityConstants;
import com.dct.model.dto.auth.UserDTO;
import com.dct.model.exception.BaseAuthenticationException;
import com.dct.model.exception.BaseBadRequestException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import io.jsonwebtoken.security.SignatureException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class JwtProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtProvider.class);
    private static final String ENTITY_NAME = "JwtProvider";
    protected final SecretKey secretKey;
    protected final JwtParser jwtParser;

    public JwtProvider(JwtProps jwtProps) {
        JwtProps jwtConfig = Optional.ofNullable(jwtProps).orElse(new JwtProps());
        String base64SecretKey = jwtConfig.getBase64SecretKey();

        if (!StringUtils.hasText(base64SecretKey)) {
            throw new RuntimeException("Could not found secret key to sign JWT");
        }

        log.debug("Using a Base64-encoded JWT secret key");
        byte[] keyBytes = Base64.getUrlDecoder().decode(base64SecretKey);
        secretKey = Keys.hmacShaKeyFor(keyBytes);
        jwtParser = Jwts.parser().verifyWith(secretKey).build();
        log.debug("Sign JWT with algorithm: {}", secretKey.getAlgorithm());
    }

    public Authentication validateToken(String token) {
        log.debug("[{}] - Validate token by default config", ENTITY_NAME);

        if (!StringUtils.hasText(token))
            throw new BaseBadRequestException(ENTITY_NAME, BaseExceptionConstants.BAD_CREDENTIALS);

        try {
            return getAuthentication(token);
        } catch (MalformedJwtException e) {
            log.error("[{}] - Invalid JWT: {}", ENTITY_NAME, e.getMessage());
        } catch (SignatureException e) {
            log.error("[{}] - Invalid JWT signature: {}", ENTITY_NAME, e.getMessage());
        } catch (SecurityException e) {
            log.error("[{}] - Unable to decode JWT: {}", ENTITY_NAME, e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("[{}] - Expired JWT: {}", ENTITY_NAME, e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("[{}] - Invalid JWT string (null, empty,...): {}", ENTITY_NAME, e.getMessage());
        }

        throw new BaseAuthenticationException(ENTITY_NAME, BaseExceptionConstants.TOKEN_INVALID_OR_EXPIRED);
    }

    public Authentication getAuthentication(String token) {
        Claims claims = (Claims) jwtParser.parse(token).getPayload();
        Integer userId = (Integer) claims.get(BaseSecurityConstants.TOKEN_PAYLOAD.USER_ID);
        String authorities = (String) claims.get(BaseSecurityConstants.TOKEN_PAYLOAD.AUTHORITIES);

        if (!StringUtils.hasText(authorities)) {
            throw new BaseAuthenticationException(ENTITY_NAME, BaseExceptionConstants.FORBIDDEN);
        }

        Collection<SimpleGrantedAuthority> userAuthorities = Arrays.stream(authorities.split(","))
                .filter(StringUtils::hasText)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());

        UserDTO principal = new UserDTO(claims.getSubject(), "none-password", userAuthorities);
        principal.setId(userId);
        return new UsernamePasswordAuthenticationToken(principal, token, userAuthorities);
    }
}
