package com.ars.gateway.config;

import com.dct.model.config.properties.JwtProps;
import com.dct.model.security.BaseJwtProvider;
import com.dct.model.security.DefaultJwtProvider;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(JwtProps.class)
public class JwtConfiguration {

    @Bean
    public BaseJwtProvider jwtProvider(JwtProps jwtProps) {
        return new DefaultJwtProvider(jwtProps);
    }
}
