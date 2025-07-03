package com.ars.gateway.security.filter;

import com.dct.model.common.JsonUtils;
import com.dct.model.dto.auth.UserDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SecurityRequestForwardingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(SecurityRequestForwardingFilter.class);
    private static final String ENTITY_NAME = "SecurityRequestForwardingFilter";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(authentication -> {
                    log.debug("[{}] - Authentication successful for user: {}", ENTITY_NAME, authentication.getName());
                    UserDTO userDTO = (UserDTO) authentication.getPrincipal();
                    Set<String> userPermissions = userDTO.getAuthorities()
                            .stream()
                            .map(GrantedAuthority::getAuthority)
                            .filter(StringUtils::hasText)
                            .collect(Collectors.toSet());

                    // Add user info vào headers để forward cho downstream services
                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                            .header("X-User-Id", String.valueOf(userDTO.getId()))
                            .header("X-User-Name", userDTO.getUsername())
                            .header("X-User-Permissions", JsonUtils.toJsonString(userPermissions))
                            .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                });
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
