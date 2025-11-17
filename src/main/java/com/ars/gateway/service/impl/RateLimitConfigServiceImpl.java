package com.ars.gateway.service.impl;

import com.ars.gateway.constants.RateLimitConstants;
import com.ars.gateway.dto.RateLimitConfigDTO;
import com.ars.gateway.security.ratelimiter.CustomRateLimiter;
import com.ars.gateway.security.ratelimiter.RateLimiterConfig;
import com.ars.gateway.service.RateLimitConfigService;
import com.dct.model.common.JsonUtils;
import com.dct.model.config.properties.SecurityProps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class RateLimitConfigServiceImpl implements RateLimitConfigService {
    private static final Logger log = LoggerFactory.getLogger(RateLimitConfigServiceImpl.class);
    private final StringRedisTemplate stringRedisTemplate;
    private final String[] defaultExcludedApis;

    public RateLimitConfigServiceImpl(StringRedisTemplate stringRedisTemplate, SecurityProps securityProps) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.defaultExcludedApis = securityProps.getRateLimitExcludedApis();
    }

    private List<RateLimitConfigDTO> getRateLimitConfig() {
        try {
            String redisConfig = stringRedisTemplate.opsForValue().get(RateLimitConstants.CONFIG_RATE_LIMIT_CODE);

            if (StringUtils.hasText(redisConfig)) {
                return JsonUtils.parseJsonToList(redisConfig, RateLimitConfigDTO.class);
            }

            return new ArrayList<>();
        } catch (Exception e) {
            log.warn("[RATE_LIMIT_CONFIG] - Failed to get RateLimiterConfig from Redis: {}", e.getMessage());
        }

        return new ArrayList<>();
    }

    private String[] getRateExcludedApis() {
        try {
            String redisConfig = stringRedisTemplate.opsForValue().get(RateLimitConstants.RATE_LIMIT_EXCLUDED_APIS);
            String[] excludedApis = null;

            if (StringUtils.hasText(redisConfig)) {
                excludedApis = JsonUtils.parseJson(redisConfig, String[].class);
            }

            return Optional.ofNullable(excludedApis).orElse(defaultExcludedApis);
        } catch (Exception e) {
            log.warn("[RATE_LIMIT_CONFIG] - Failed to get excluded APIs from Redis: {}", e.getMessage());
            return defaultExcludedApis;
        }
    }

    @Override
    public boolean refreshRateLimitConfig() {
        List<RateLimitConfigDTO> rateLimitConfigDTOs = getRateLimitConfig();

        if (rateLimitConfigDTOs.isEmpty()) {
            return false;
        }

        for (RateLimitConfigDTO rateLimitConfigDTO : rateLimitConfigDTOs) {
            RateLimiterConfig rateLimiterConfig = new RateLimiterConfig();
            rateLimiterConfig.setBanThreshold(rateLimitConfigDTO.getBanThreshold());
            rateLimiterConfig.setWindowSeconds(rateLimitConfigDTO.getWindowSeconds());
            rateLimiterConfig.setBanDurationMinutes(rateLimitConfigDTO.getBanDurationMinutes());
            CustomRateLimiter.updateRateLimiterConfig(rateLimitConfigDTO.getRouteId(), rateLimiterConfig);
            log.info(
                "[REFRESH_RATE_LIMITER_CONFIG_INFO] - routeId: {}, value: {}",
                rateLimitConfigDTO.getRouteId(),
                rateLimiterConfig
            );
        }

        return true;
    }

    @Override
    public void refreshRateLimitExcludedApis() {
        String[] excludedApis = getRateExcludedApis();
        CustomRateLimiter.updateRateExcludedApis(excludedApis);
        log.info("[REFRESH_RATE_LIMITER_EXCLUDED_API_CONFIG_INFO] - value: {}", Arrays.toString(excludedApis));
    }

    @Override
    public List<String> getDefaultRateExcludedApis() {
        return List.of(defaultExcludedApis);
    }
}
