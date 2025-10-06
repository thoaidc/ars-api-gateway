package com.ars.gateway.service.impl;

import com.ars.gateway.dto.RateLimitConfigDTO;
import com.ars.gateway.security.ratelimiter.CustomRateLimiter;
import com.ars.gateway.security.ratelimiter.RateLimiterConfig;
import com.ars.gateway.service.RateLimitConfigService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RateLimitConfigServiceImpl implements RateLimitConfigService {
    private static final Logger log = LoggerFactory.getLogger(RateLimitConfigServiceImpl.class);

    private List<RateLimitConfigDTO> getRateLimitConfig() {
        // Change to get from Redis
//        Optional<SystemConfig> systemConfigs = configRepository.findByCode(RateLimitConstants.CONFIG_RATE_LIMIT_CODE);
//
//        if (systemConfigs.isPresent()) {
//            String value = systemConfigs.get().getValue();
//            return JsonUtils.parseJsonToList(value, RateLimitConfigDTO.class);
//        }

        return new ArrayList<>();
    }

    @Override
    public void updateRateLimitConfig() {
        List<RateLimitConfigDTO> rateLimitConfigDTOs = this.getRateLimitConfig();
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
    }
}
