package com.ars.gateway.security.ratelimiter;

import com.ars.gateway.constants.RateLimitConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.ratelimit.AbstractRateLimiter;
import org.springframework.cloud.gateway.support.ConfigurationService;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Primary
@Component
public class CustomRateLimiter extends AbstractRateLimiter<RateLimiterConfig> {
    private static final Logger log = LoggerFactory.getLogger(CustomRateLimiter.class);
    private static final Map<String, RateLimiterConfig> rateLimiterConfigs = new ConcurrentHashMap<>();
    private final StringRedisTemplate redisTemplate;

    public CustomRateLimiter(StringRedisTemplate redisTemplate, ConfigurationService configService) {
        super(RateLimiterConfig.class, RateLimitConstants.RATE_LIMIT_PROPERTIES_PREFIX, configService);
        this.redisTemplate = redisTemplate;
    }

    // For dynamic update config in runtime
    public static void updateRateLimiterConfig(String key, RateLimiterConfig rateLimiterConfig) {
        rateLimiterConfigs.put(key, rateLimiterConfig);
    }

    /**
     * Checks whether the client is allowed to make a request.
     *
     * @param routeId The route ID to fetch the rate limit configuration.
     * @param id      The client identifier (e.g., userId, IP) to distinguish clients.
     * @return Mono<Response> containing {@code allowed = true/false} and optional metadata.
     */
    @Override
    public Mono<Response> isAllowed(String routeId, String id) {
        return Mono.fromSupplier(() -> {
            // Retrieve the rate limiter configuration for this route
            RateLimiterConfig rateLimiterConfig = rateLimiterConfigs.getOrDefault(routeId, getConfig().get(routeId));
            // Redis key to mark temporarily banned clients
            String clientBanned = RateLimitConstants.BAN_KEY_PREFIX + id;

            // If the client is currently banned, return not allowed
            if (redisTemplate.hasKey(clientBanned)) {
                log.info("[REQUEST_BANNED] - currently banned device: {}", clientBanned);
                return new Response(RateLimitConstants.REQUEST_BANNED, Collections.emptyMap());
            }

            // Redis key to count requests within a fixed window
            String requestRateLimitKey = RateLimitConstants.RATE_LIMIT_KEY + routeId + id;
            Long requestCounted = redisTemplate.opsForValue().increment(requestRateLimitKey);
            // Set the window expiration (e.g., 60 seconds)
            redisTemplate.expire(requestRateLimitKey, Duration.ofSeconds(rateLimiterConfig.getWindowSeconds()));

            // If the request count exceeds the threshold, ban the client temporarily
            if (Objects.nonNull(requestCounted) && requestCounted > rateLimiterConfig.getBanThreshold()) {
                log.info("[REQUEST_NOW_BANNED] - start banning device: {}", clientBanned);
                Duration blockingTime = Duration.ofMinutes(rateLimiterConfig.getBanDurationMinutes());
                redisTemplate.opsForValue().set(clientBanned, RateLimitConstants.BANNED_VALUE, blockingTime);
                return new Response(RateLimitConstants.REQUEST_BANNED, Collections.emptyMap());
            }

            // Otherwise, allow the request
            return new Response(RateLimitConstants.REQUEST_ALLOWED, Collections.emptyMap());
        });
    }
}
