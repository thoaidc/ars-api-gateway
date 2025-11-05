package com.ars.gateway.security.config;

import com.ars.gateway.security.filter.JwtFilter;
import com.dct.model.config.properties.CorsProps;
import com.dct.model.config.properties.SecurityProps;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@EnableConfigurationProperties({SecurityProps.class, CorsProps.class})
public class GatewaySecurityConfig {
    private final DynamicPublicRequestContext dynamicPublicRequestContext;
    private final ServerAuthenticationEntryPoint authenticationEntryPoint;
    private final ServerAccessDeniedHandler accessDeniedHandler;
    private final JwtFilter jwtFilter;
    private final CorsProps corsProps;

    public GatewaySecurityConfig(DynamicPublicRequestContext dynamicPublicRequestContext,
                                 ServerAuthenticationEntryPoint authenticationEntryPoint,
                                 ServerAccessDeniedHandler accessDeniedHandler,
                                 JwtFilter jwtFilter,
                                 CorsProps corsProps) {
        this.dynamicPublicRequestContext = dynamicPublicRequestContext;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.jwtFilter = jwtFilter;
        this.corsProps = corsProps;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .headers(headers -> headers
                    .frameOptions(Customizer.withDefaults())
                    .contentTypeOptions(Customizer.withDefaults())
                )
                .addFilterBefore(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .authorizeExchange(exchanges ->
                    exchanges.anyExchange().access(dynamicPublicRequestContext::isPublicPath)
                )
                .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler)
                )
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        corsProps.getPatterns().forEach((path, config) -> {
            CorsConfiguration cors = new CorsConfiguration();
            List<String> allowedOriginPatterns = config.getAllowedOriginPatterns();

            if (Objects.nonNull(allowedOriginPatterns) && !allowedOriginPatterns.isEmpty()) {
                cors.setAllowedOriginPatterns(allowedOriginPatterns);
            } else {
                cors.setAllowedOrigins(config.getAllowedOrigins());
            }

            cors.setAllowedMethods(config.getAllowedMethods());
            cors.setAllowedHeaders(config.getAllowedHeaders());
            cors.setAllowCredentials(config.getAllowCredentials());
            cors.setMaxAge(Duration.ofSeconds(config.getMaxAge()));
            source.registerCorsConfiguration(path, cors);
        });

        return source;
    }
}
