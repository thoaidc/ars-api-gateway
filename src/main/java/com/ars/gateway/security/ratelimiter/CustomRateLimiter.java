package com.ars.gateway.security.ratelimiter;

import com.ars.gateway.constants.RateLimitConstants;
import com.dct.model.common.SecurityUtils;
import com.dct.model.config.properties.SecurityProps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.ratelimit.AbstractRateLimiter;
import org.springframework.cloud.gateway.support.ConfigurationService;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.ars.gateway.constants.RateLimitConstants.RATE_LIMIT_EXCLUDED_APIS;

@Primary
@Component
public class CustomRateLimiter extends AbstractRateLimiter<RateLimiterConfig> {
    private static final Logger log = LoggerFactory.getLogger(CustomRateLimiter.class);
    private static final Map<String, RateLimiterConfig> rateLimiterConfigs = new ConcurrentHashMap<>();
    private static final Map<String, String[]> rateLimitExcludedApis = new ConcurrentHashMap<>();
    private final StringRedisTemplate redisTemplate;
    private final String[] defaultExcludedApis;
    private final RedisScript<Long> rateLimitScript;
    // Lua script to ensure atomic increment + expire
    private static final String LUA_RATE_LIMIT_SCRIPT = """
        local current = redis.call('INCR', KEYS[1])
        if current == 1 then
            redis.call('EXPIRE', KEYS[1], ARGV[1])
        end
        return current
    """;

    public CustomRateLimiter(StringRedisTemplate redisTemplate,
                             ConfigurationService configService,
                             SecurityProps securityProps) {
        super(RateLimiterConfig.class, RateLimitConstants.RATE_LIMIT_PROPERTIES_PREFIX, configService);
        this.redisTemplate = redisTemplate;
        this.defaultExcludedApis = securityProps.getRateLimitExcludedApis();
        this.rateLimitScript = new DefaultRedisScript<>(LUA_RATE_LIMIT_SCRIPT, Long.class);
    }

    // For dynamic update config in runtime
    public static void updateRateLimiterConfig(String key, RateLimiterConfig rateLimiterConfig) {
        rateLimiterConfigs.put(key, rateLimiterConfig);
    }

    // For dynamic update config in runtime
    public static void updateRateExcludedApis(String[] excludedApis) {
        rateLimitExcludedApis.put(RATE_LIMIT_EXCLUDED_APIS, excludedApis);
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
        return Mono.fromCallable(() -> {
            String[] excludedApis = rateLimitExcludedApis.getOrDefault(RATE_LIMIT_EXCLUDED_APIS, defaultExcludedApis);

            if (SecurityUtils.checkPathMatches(routeId, excludedApis)) {
                return new Response(RateLimitConstants.REQUEST_ALLOWED, Collections.emptyMap());
            }

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
            String requestRateLimitKey = RateLimitConstants.RATE_LIMIT_KEY + routeId + ":" + id;
            // Atomic increment + expire using Lua script. Set the window expiration (e.g., 1 seconds)
            Long requestCounted = redisTemplate.execute(
                rateLimitScript,
                Collections.singletonList(requestRateLimitKey),
                String.valueOf(rateLimiterConfig.getWindowSeconds())
            );

            // If the request count exceeds the threshold, ban the client temporarily
            if (requestCounted > rateLimiterConfig.getBanThreshold()) {
                log.info("[REQUEST_NOW_BANNED] - start banning device: {}", clientBanned);
                Duration blockingTime = Duration.ofMinutes(rateLimiterConfig.getBanDurationMinutes());
                redisTemplate.opsForValue().set(clientBanned, RateLimitConstants.BANNED_VALUE, blockingTime);
                return new Response(RateLimitConstants.REQUEST_BANNED, Collections.emptyMap());
            }

            // Otherwise, allow the request
            return new Response(RateLimitConstants.REQUEST_ALLOWED, Collections.emptyMap());
        }).onErrorResume(exception -> {
            log.error("[RATE_LIMITER_REDIS_ERROR] - Allowed requests because Redis error: {}", exception.getMessage());
            return Mono.just(new Response(RateLimitConstants.REQUEST_ALLOWED, Collections.emptyMap()));
        });
    }
}
