package com.ars.gateway.common;

import com.ars.gateway.config.properties.CacheProps;
import com.ars.gateway.constants.PropertiesConstants;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = PropertiesConstants.ENABLED_CACHE, havingValue = "true")
@EnableConfigurationProperties(CacheProps.class)
public class CacheUtils {

    private static final Logger log = LoggerFactory.getLogger(CacheUtils.class);
    private static final String ENTITY_NAME = "CacheUtils";
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final CacheProps cacheProps;

    public CacheUtils(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper, CacheProps cacheProps) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.cacheProps = cacheProps;
    }

    public String cache(Object data) {
        String hashedKey = DigestUtils.md5DigestAsHex(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        cache(hashedKey, data, cacheProps.getTtlMinutes());
        return hashedKey;
    }

    public void cache(String key, Object data) {
        cache(key, data, cacheProps.getTtlMinutes());
    }

    public void cache(String key, Object data, int ttlMinutes) {
        try {
            String hashedKey = DigestUtils.md5DigestAsHex(key.getBytes(StandardCharsets.UTF_8));
            String jsonData = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(hashedKey, jsonData, Duration.ofMinutes(ttlMinutes));
            log.debug("[{}] - Cached data with key: {}", ENTITY_NAME, hashedKey);
        } catch (Exception e) {
            log.warn("[{}] - Failed to cache data: {}", ENTITY_NAME, e.getMessage());
        }
    }

    public String getCache(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public <T> T getCache(String key, Class<T> type) {
        String cachedData = getCache(key);

        try {
            return objectMapper.readValue(cachedData, type);
        } catch (JsonProcessingException e) {
            log.warn("[{}] - Failed to parse cache data: {}", ENTITY_NAME, e.getMessage());
            return null;
        }
    }
}
