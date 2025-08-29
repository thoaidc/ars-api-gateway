package com.ars.gateway.config;

import com.dct.model.config.properties.RedisProps;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableRedisRepositories
@EnableConfigurationProperties(RedisProps.class)
public class RedisConfiguration {
    private final RedisProps redisProps;

    public RedisConfiguration(RedisProps redisProps) {
        this.redisProps = redisProps;
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisProps.getHost());
        redisConfig.setPort(redisProps.getPort());
        redisConfig.setDatabase(redisProps.getDatabase());
        redisConfig.setUsername(redisProps.getUsername());
        redisConfig.setPassword(redisProps.getPassword());

        SocketOptions socketOptions = SocketOptions.builder()
                .connectTimeout(Duration.ofMillis(redisProps.getConnectionTimeout()))
                .build();

        ClientOptions clientOptions = ClientOptions.builder()
                .autoReconnect(redisProps.isAutoReconnect())
                .socketOptions(socketOptions)
                .build();

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(redisProps.getCommandTimeout()))
                .shutdownTimeout(Duration.ofMillis(redisProps.getShutdownTimeout()))
                .clientOptions(clientOptions)
                .build();

        return new LettuceConnectionFactory(redisConfig, clientConfig);
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}
