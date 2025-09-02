package com.ars.gateway.security.filter;

import com.ars.gateway.constants.CommonConstants;
import com.dct.model.common.SecurityUtils;
import com.dct.model.config.properties.SecurityProps;
import com.dct.model.constants.BaseSecurityConstants;
import com.dct.model.dto.auth.BaseUserDTO;
import com.dct.model.exception.BaseAuthenticationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.server.authorization.AuthorizationWebFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Global filter responsible for forwarding user authentication details
 * to downstream microservices via custom request headers.
 *
 * <p>This filter performs the following:
 * <ul>
 *   <li>Runs only after a request has been successfully authenticated and authorized.</li>
 *   <li>Extracts the {@link Authentication} object (set earlier by JwtFilter).</li>
 *   <li>Mutates the request by adding user-specific headers such as:
 *       <ul>
 *           <li>{@code X-User-Id}</li>
 *           <li>{@code X-User-Name}</li>
 *           <li>{@code X-User-Permissions} (as JSON string)</li>
 *       </ul>
 *   </li>
 *   <li>Skips processing if no authenticated user is found (unauthorized request).</li>
 *   <li>Does not apply to public or permitAll() endpoints.</li>
 * </ul>
 *
 * <p>This filter is executed after Spring Security's {@link AuthorizationWebFilter}
 *
 * @author thoaidc
 */
@Component
public class SecurityRequestForwardingFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(SecurityRequestForwardingFilter.class);
    private static final String ENTITY_NAME = "com.ars.gateway.security.filter.SecurityRequestForwardingFilter";
    private final String[] publicPatterns;

    public SecurityRequestForwardingFilter(SecurityProps securityProps) {
        this.publicPatterns = Optional.ofNullable(securityProps).orElseGet(SecurityProps::new).getPublicRequestPatterns();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestUri = exchange.getRequest().getURI().getPath();

        if (SecurityUtils.checkIfAuthenticationNotRequired(requestUri, publicPatterns)) {
            return chain.filter(exchange);
        }

        log.debug("[HEADER_SECURITY_FORWARD] - Forward request: {}: {}", exchange.getRequest().getMethod(), requestUri);
        Authentication authentication = exchange.getAttribute(CommonConstants.AUTHENTICATION_EXCHANGE_ATTRIBUTE);

        if (Objects.isNull(authentication)) {
            return Mono.error(new BaseAuthenticationException(ENTITY_NAME, "Authentication not found!"));
        }

        BaseUserDTO userDTO = (BaseUserDTO) authentication.getPrincipal();
        Set<String> userPermissions = userDTO.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(BaseSecurityConstants.HEADER.USER_ID, String.valueOf(userDTO.getId()))
                .header(BaseSecurityConstants.HEADER.USER_NAME, userDTO.getUsername())
                .header(BaseSecurityConstants.HEADER.USER_AUTHORITIES, String.join(",", userPermissions))
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        // Set Filter order right after AuthorizationWebFilter
        return SecurityWebFiltersOrder.AUTHORIZATION.getOrder() + 1;
    }
}
