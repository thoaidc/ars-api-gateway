package com.ars.gateway.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class CacheUtils {
    private static final Logger log = LoggerFactory.getLogger(CacheUtils.class);
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public CacheUtils(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public String hashKey(String key) {
        return DigestUtils.md5DigestAsHex(key.getBytes(StandardCharsets.UTF_8));
    }

    public void cache(String key, Object data) {
        cache(key, data, 5);
    }

    public void cache(String key, Object data, int ttlMinutes) {
        try {
            String hashKey = hashKey(key);
            String jsonData = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(hashKey, jsonData, Duration.ofMinutes(ttlMinutes));
            log.debug("[CACHED_DATA] - Cached data with key: {}", hashKey);
        } catch (Exception e) {
            log.warn("[CACHED_DATA_ERROR] - Failed to cache data: {}", e.getMessage());
        }
    }

    public String get(String key) {
        String hashedKey = hashKey(key);

        try {
            return redisTemplate.opsForValue().get(hashedKey);
        } catch (RedisConnectionFailureException e) {
            log.error("[REDIS_CONNECTION_ERROR] - Cannot connect to Redis: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("[REDIS_GET_ERROR] - Failed to get key {}: {}", hashedKey, e.getMessage());
            return null;
        }
    }

    public <T> T get(String key, Class<T> type) {
        String cachedData = get(key);

        try {
            return objectMapper.readValue(cachedData, type);
        } catch (Exception e) {
            log.warn("[PARSE_CACHED_DATA_ERROR] - Failed to parse cached data: {}", e.getMessage());
            evict(key);
            return null;
        }
    }

    public void evict(String key) {
        try {
            String hashedKey = hashKey(key);
            redisTemplate.delete(hashedKey);
            log.debug("[EVICTED_CACHE] - Evicted cache for key: {}", hashedKey);
        } catch (Exception e) {
            log.warn("[EVICTED_CACHE_ERROR] - Failed to evict cache for key {}: {}", key, e.getMessage());
        }
    }
}
