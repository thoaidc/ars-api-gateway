package com.ars.gateway.security;

import com.ars.gateway.config.properties.AuthenticationCacheProps;
import com.ars.gateway.config.properties.PublicEndpointProps;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;

@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties({AuthenticationCacheProps.class, PublicEndpointProps.class})
public class GatewaySecurityConfig {

    private final ServerAuthenticationEntryPoint authenticationEntryPoint;
    private final ServerAccessDeniedHandler accessDeniedHandler;
    private final PublicEndpointProps publicEndpointProps;

    public GatewaySecurityConfig(ServerAuthenticationEntryPoint authenticationEntryPoint,
                                 ServerAccessDeniedHandler accessDeniedHandler,
                                 PublicEndpointProps publicEndpointProps) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.publicEndpointProps = publicEndpointProps;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http.cors(ServerHttpSecurity.CorsSpec::disable)
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .headers(headers -> headers
                    .frameOptions(ServerHttpSecurity.HeaderSpec.FrameOptionsSpec::disable)
                    .contentTypeOptions(Customizer.withDefaults())
                )
                .authorizeExchange(exchanges -> {
                    exchanges.pathMatchers(publicEndpointProps.getPublicPatterns().toArray(new String[0])).permitAll();
                    exchanges.anyExchange().permitAll();
                })
                .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler)
                )
                .build();
    }
}
